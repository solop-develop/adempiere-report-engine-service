package org.spin.service.grpc.authentication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Determines whether a JWT token was issued by Keycloak or by ADempiere (local).
 * Decodes only the payload (Base64) without validating the signature,
 * since nginx + OAuth2 Proxy already validated Keycloak tokens.
 *
 * Configuration via environment variables:
 * - KEYCLOAK_AUTH_ENABLED: "true" to enable detection (default: "false")
 * - KEYCLOAK_HOST: Keycloak server hostname (e.g. "auth.example.com")
 * - KEYCLOAK_REALM: Keycloak realm name (e.g. "dyd")
 *
 * The issuer URI is composed as: https://{KEYCLOAK_HOST}/realms/{KEYCLOAK_REALM}
 */
public class TokenTypeDetector {

	private static final Logger log = Logger.getLogger(TokenTypeDetector.class.getName());

	public enum TokenType {
		LOCAL,
		KEYCLOAK
	}

	/**
	 * Detect the token type by inspecting the JWT "iss" claim.
	 * @param token JWT string without "Bearer " prefix
	 * @return TokenType.KEYCLOAK if issuer matches, TokenType.LOCAL otherwise
	 */
	public static TokenType detect(String token) {
		if (!isKeycloakEnabled()) {
			return TokenType.LOCAL;
		}
		String expectedIssuer = getKeycloakIssuerUri();
		if (expectedIssuer == null || expectedIssuer.isBlank()) {
			return TokenType.LOCAL;
		}
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				return TokenType.LOCAL;
			}
			String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
			JsonObject claims = JsonParser.parseString(payload).getAsJsonObject();
			String issuer = claims.has("iss") && !claims.get("iss").isJsonNull()
				? claims.get("iss").getAsString()
				: null;
			if (expectedIssuer.equals(issuer)) {
				log.fine("Token identified as Keycloak (issuer: " + issuer + ")");
				return TokenType.KEYCLOAK;
			}
		} catch (Exception e) {
			log.log(Level.FINE, "Could not decode token for type detection, assuming LOCAL: " + e.getMessage());
		}
		return TokenType.LOCAL;
	}

	private static boolean isKeycloakEnabled() {
		String enabled = System.getenv("KEYCLOAK_AUTH_ENABLED");
		return "true".equalsIgnoreCase(enabled);
	}

	/**
	 * Composes the Keycloak issuer URI from KEYCLOAK_HOST and KEYCLOAK_REALM.
	 * Result: https://{host}/realms/{realm}
	 */
	private static String getKeycloakIssuerUri() {
		String host = System.getenv("KEYCLOAK_HOST");
		String realm = System.getenv("KEYCLOAK_REALM");
		if (host == null || host.isBlank() || realm == null || realm.isBlank()) {
			return null;
		}
		return "https://" + host + "/realms/" + realm;
	}
}
