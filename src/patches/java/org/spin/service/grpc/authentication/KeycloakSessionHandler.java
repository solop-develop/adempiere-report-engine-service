package org.spin.service.grpc.authentication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPreference;
import org.compiere.model.MRole;
import org.compiere.model.MSession;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.service.grpc.util.base.PreferenceUtil;

import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * Resolves an ADempiere session from a pre-validated Keycloak JWT.
 * <p>
 * The JWT was already validated by nginx + OAuth2 Proxy, so this handler
 * only decodes the payload (Base64) to extract claims, then maps the
 * Keycloak identity to an existing ADempiere user.
 * <p>
 * Sessions are cached by Keycloak session ID (sid) to avoid creating
 * a new ADempiere session on every request.
 * <p>
 * This is the gRPC equivalent of {@code com.sabana.auth.services.KeycloakSessionService}
 * but without Spring dependencies — all static methods, plain Java.
 */
public class KeycloakSessionHandler {

	private static final CLogger log = CLogger.getCLogger(KeycloakSessionHandler.class);

	/** Cache: Keycloak sid -> ADempiere AD_Session_ID.
	 *  Name must NOT start with "AD_Session" to avoid being wiped
	 *  by CacheMgt.reset("AD_Session") on every MSession save. */
	private static final CCache<String, Integer> keycloakSessionCache =
		new CCache<>("KeycloakSidMapping", 100, 60); // 60 min timeout

	/**
	 * Resolves an ADempiere session context from a Keycloak JWT.
	 * <p>
	 * 1. Decode JWT payload (Base64, no signature validation)
	 * 2. Extract email, preferred_username, and sid (Keycloak session ID)
	 * 3. Check if an ADempiere session already exists for this Keycloak sid
	 * 4. If yes, reload the existing session context
	 * 5. If no, find AD_User, resolve defaults, create new MSession
	 * 6. Build full context with preferences and defaults
	 *
	 * @param token JWT string without "Bearer " prefix
	 * @return Properties context with session data
	 */
	public static Properties resolveSession(String token) {
		KeycloakClaims claims = decodeClaims(token);
		log.fine("Keycloak claims — sub: " + claims.sub + ", email: " + claims.email
			+ ", username: " + claims.preferredUsername + ", sid: " + claims.sessionId);

		if (claims.sessionId == null || claims.sessionId.isBlank()) {
			throw new AdempiereException("Keycloak JWT missing 'sid' claim");
		}

		// 1. Fast path: in-memory cache
		Integer existingSessionId = keycloakSessionCache.get(claims.sessionId);
		if (existingSessionId != null && existingSessionId > 0) {
			Properties context = loadExistingSessionContext(existingSessionId);
			if (context != null) {
				log.fine("Reusing cached ADempiere session " + existingSessionId
					+ " for Keycloak sid: " + claims.sessionId);
				return context;
			}
			keycloakSessionCache.remove(claims.sessionId);
		}

		// 2. DB fallback: recover session after restart or cache eviction
		int dbSessionId = findSessionIdByKeycloakSid(claims.sessionId);
		if (dbSessionId > 0) {
			Properties context = loadExistingSessionContext(dbSessionId);
			if (context != null) {
				keycloakSessionCache.put(claims.sessionId, dbSessionId); // re-warm cache
				log.fine("Recovered ADempiere session " + dbSessionId
					+ " from DB for Keycloak sid: " + claims.sessionId);
				return context;
			}
		}

		// 3. No valid session anywhere — create a new one
		return createNewSessionContext(claims);
	}

	/**
	 * Load context from an existing ADempiere session.
	 * Returns null if the session is no longer valid.
	 */
	private static Properties loadExistingSessionContext(int sessionId) {
		MSession session = new MSession(Env.getCtx(), sessionId, null);
		if (session.getAD_Session_ID() <= 0 || session.isProcessed()) {
			return null;
		}

		// The Keycloak flow tags AD_Session.WebSession with "keycloak:<sid>" so the
		// DB-fallback lookup can find it. A reused AD_Session_ID with a different
		// tag means another flow created the row — likely the sid-cache mapping
		// got corrupted and we are about to load the wrong user's session.
		String webSession = session.getWebSession();
		if (webSession == null || !webSession.startsWith("keycloak:")) {
			log.warning("Keycloak session resolve loaded AD_Session_ID=" + sessionId
				+ " with unexpected WebSession='" + webSession + "' — refusing reuse");
			return null;
		}

		Properties context = (Properties) Env.getCtx().clone();
		DB.validateSupportedUUIDFromDB();

		// Set context from existing session
		Env.setContext(context, "#AD_Session_ID", session.getAD_Session_ID());
		Env.setContext(context, "#Session_UUID", session.getUUID());
		Env.setContext(context, "#AD_User_ID", session.getCreatedBy());
		Env.setContext(context, "#AD_Role_ID", session.getAD_Role_ID());
		Env.setContext(context, "#AD_Client_ID", session.getAD_Client_ID());
		Env.setContext(context, "#AD_Org_ID", session.getAD_Org_ID());
		Env.setContext(context, "#Date", new Timestamp(System.currentTimeMillis()));

		// Force-reload the role so its cached access SQL is rebuilt with the
		// current tenant context — protects against role instances cached with
		// a stale #AD_Client_ID (e.g. left over from a System → Company switch).
		MRole.removeFromCache(session.getAD_Role_ID(), session.getCreatedBy());
		MRole.get(context, session.getAD_Role_ID(), session.getCreatedBy(), true);

		int orgId = session.getAD_Org_ID();

		// Load language and warehouse from saved preferences (written by setSessionAttribute / runChangeRole)
		// Cannot rely on session.getCtx() because MSession has no AD_Language column —
		// getCtx() returns the global context, not the session-specific one.
		int userId = session.getCreatedBy();
		String language = null;
		int warehouseId = -1;
		List<MPreference> preferences = PreferenceUtil.getSessionPreferences(userId);
		for (MPreference pref : preferences) {
			if (PreferenceUtil.P_LANGUAGE.equals(pref.getAttribute())) {
				language = pref.getValue();
			} else if (PreferenceUtil.P_WAREHOUSE.equals(pref.getAttribute())) {
				warehouseId = Integer.parseInt(pref.getValue());
			}
		}
		if (Util.isEmpty(language, true)) {
			language = SessionManager.getDefaultLanguage(null);
		}
		if (warehouseId < 0) {
			warehouseId = SessionManager.getDefaultWarehouseId(orgId);
		}
		if (warehouseId < 0) {
			warehouseId = 0;
		}
		Env.setContext(context, "#M_Warehouse_ID", warehouseId);
		// Load preferences and defaults
		SessionManager.loadDefaultSessionValues(context, language);

		return context;
	}

	/**
	 * Query AD_Session by WebSession column for DB-backed recovery after server restart.
	 * Returns the most recently created valid session ID, or -1 if not found.
	 */
	private static int findSessionIdByKeycloakSid(String keycloakSid) {
		int result = DB.getSQLValue(
			null,
			"SELECT AD_Session_ID FROM AD_Session "
				+ "WHERE WebSession = ? AND Processed = 'N' "
				+ "ORDER BY Created DESC LIMIT 1",
			"keycloak:" + keycloakSid
		);
		return result > 0 ? result : -1;
	}

	/**
	 * Create a new ADempiere session for a Keycloak user.
	 * Maps Keycloak identity to AD_User, resolves defaults, and creates MSession
	 * with the Keycloak sid stored in WebSession for traceability.
	 */
	private static Properties createNewSessionContext(KeycloakClaims claims) {
		int userId = findAdempiereUserId(claims);

		int roleId = SessionManager.getDefaultRoleId(userId);
		if (roleId < 0) {
			throw new AdempiereException(
				"@AD_Role_ID@ @NotFound@ for Keycloak user: " + claims.email);
		}

		int orgId = SessionManager.getDefaultOrganizationId(roleId, userId);
		if (orgId < 0) {
			orgId = 0;
		}

		int warehouseId = SessionManager.getDefaultWarehouseId(orgId);
		if (warehouseId < 0) {
			warehouseId = 0;
		}

		String language = SessionManager.getDefaultLanguage(null);

		// Create session using SessionManager
		MSession session = SessionManager.createSession(
			"grpc-keycloak",
			language,
			roleId,
			userId,
			orgId,
			warehouseId
		);

		// Store Keycloak sid in WebSession for traceability
		session.setWebSession("keycloak:" + claims.sessionId);
		session.saveEx();

		// Cache the mapping: Keycloak sid -> ADempiere session ID
		keycloakSessionCache.put(claims.sessionId, session.getAD_Session_ID());

		// Build context following getSessionFromToken pattern
		Properties context = session.getCtx();
		Env.setContext(context, "#AD_Session_ID", session.getAD_Session_ID());
		Env.setContext(context, "#Session_UUID", session.getUUID());
		Env.setContext(context, "#AD_User_ID", session.getCreatedBy());
		Env.setContext(context, "#AD_Role_ID", session.getAD_Role_ID());
		Env.setContext(context, "#AD_Client_ID", session.getAD_Client_ID());
		Env.setContext(context, "#AD_Org_ID", orgId);
		Env.setContext(context, "#M_Warehouse_ID", warehouseId);
		Env.setContext(context, "#Date", new Timestamp(System.currentTimeMillis()));
		Env.setContext(context, Env.LANGUAGE, SessionManager.getDefaultLanguage(language));

		// Load preferences and defaults
		SessionManager.loadDefaultSessionValues(context, language);

		log.info("Created new ADempiere session " + session.getAD_Session_ID()
			+ " for Keycloak sid: " + claims.sessionId
			+ ", user: " + claims.email + " (AD_User_ID=" + userId + ")");

		return context;
	}

	/**
	 * Decode JWT payload (Base64, no signature validation) and extract Keycloak claims.
	 */
	private static KeycloakClaims decodeClaims(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw new AdempiereException("Invalid JWT structure");
			}
			String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
			JsonObject node = JsonParser.parseString(payload).getAsJsonObject();
			return new KeycloakClaims(
				getTextOrNull(node, "sub"),
				getTextOrNull(node, "email"),
				getTextOrNull(node, "preferred_username"),
				getTextOrNull(node, "name"),
				getTextOrNull(node, "sid")
			);
		} catch (AdempiereException e) {
			throw e;
		} catch (Exception e) {
			throw new AdempiereException("Failed to decode Keycloak JWT: " + e.getMessage());
		}
	}

	private static String getTextOrNull(JsonObject node, String field) {
		return node.has(field) && !node.get(field).isJsonNull()
			? node.get(field).getAsString()
			: null;
	}

	/**
	 * Find ADempiere user matching Keycloak identity.
	 * Priority: email > username (preferred_username).
	 */
	private static int findAdempiereUserId(KeycloakClaims claims) {
		// Priority 1: find by email (case-insensitive)
		if (!Util.isEmpty(claims.email, true)) {
			int userId = DB.getSQLValue(
				null,
				"SELECT AD_User_ID FROM AD_User WHERE LOWER(EMail) = LOWER(?) AND IsActive = 'Y' LIMIT 1",
				claims.email
			);
			if (userId > 0) {
				return userId;
			}
		}
		// Priority 2: find by username (Value column in AD_User, case-insensitive)
		if (!Util.isEmpty(claims.preferredUsername, true)) {
			int userId = DB.getSQLValue(
				null,
				"SELECT AD_User_ID FROM AD_User WHERE LOWER(Value) = LOWER(?) AND IsActive = 'Y' LIMIT 1",
				claims.preferredUsername
			);
			if (userId > 0) {
				return userId;
			}
		}
		throw new AdempiereException(
			"ADempiere user not found for Keycloak identity — email: "
				+ claims.email + ", username: " + claims.preferredUsername);
	}

	/**
	 * Updates the Keycloak sid -> AD_Session_ID cache mapping.
	 * Called after change-role creates a new ADempiere session.
	 */
	public static void updateSessionCache(String keycloakSid, int newSessionId) {
		if (keycloakSid == null || keycloakSid.isBlank() || newSessionId <= 0) {
			return;
		}
		keycloakSessionCache.put(keycloakSid, newSessionId);
		log.fine("Updated Keycloak session cache — sid: " + keycloakSid
			+ " → AD_Session_ID: " + newSessionId);
	}

	/**
	 * Extracts the Keycloak session ID (sid) from a JWT token.
	 * Returns null if the token is not a valid Keycloak JWT or has no sid.
	 */
	public static String extractSessionId(String token) {
		try {
			KeycloakClaims claims = decodeClaims(token);
			return claims.sessionId;
		} catch (Exception e) {
			return null;
		}
	}

	/** Keycloak JWT claims */
	private static class KeycloakClaims {
		final String sub;
		final String email;
		final String preferredUsername;
		final String name;
		final String sessionId;

		KeycloakClaims(String sub, String email, String preferredUsername, String name, String sessionId) {
			this.sub = sub;
			this.email = email;
			this.preferredUsername = preferredUsername;
			this.name = name;
			this.sessionId = sessionId;
		}
	}
}
