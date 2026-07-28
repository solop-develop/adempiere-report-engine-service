/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2015 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.spin.eca52.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.adempiere.core.domains.models.*;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.SecureEngine;
import org.compiere.util.Util;
import org.spin.eca52.util.JWTUtil;
import org.spin.model.MADToken;
import org.spin.model.MADTokenDefinition;
import org.spin.util.IThirdPartyAccessGenerator;

import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple token generator for third party access
 * Note: You should generate your own token generator
 * @author Yamel Senih, ySenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class JWT implements IThirdPartyAccessGenerator {

	/**	Default Token	*/
	private MADToken token = null;
	/**	User Token value	*/
	private String userTokenValue = null;
	/**	Language	*/
	private String language;
	/**	Session ID	*/
	private int sessionId;
	/**	SysConfig (ms): maximum lifetime for third party grant tokens; caps even a longer value set on the token definition	*/
	private static final String ECA52_TOKEN_MAX_EXPIRE_TIME = "ECA52_TOKEN_MAX_EXPIRE_TIME";
	/**	Default maximum grant-token lifetime: 90 days in ms	*/
	private static final long DEFAULT_MAX_TOKEN_EXPIRE_MILLIS = 90L * 24 * 60 * 60 * 1000;
	/**	Default grant-token lifetime when the definition expires but sets no explicit time: 5 min in ms	*/
	private static final long DEFAULT_TOKEN_EXPIRE_MILLIS = 5L * 60 * 1000;

	public JWT() {
    	//	
    }
	
    @Override
	public String generateToken(String tokenType, int userId) {
    	throw new AdempiereException("@Unsupported@");
	}

	
	@Override
	public boolean validateToken(String token, int userId) {
		return false;
	}
	
	@Override
	public MADToken getToken() {
		return token;
	}
	
	@Override
	public String getTokenValue() {
		return userTokenValue;
	}

	/**
	 * Get JWT Secrect Key generates with HMAC-SHA algorithms
	 * @return
	 */
	private static String getJWT_SecretKeyAsString(int clientId) {
		// get by SysConfig client
		String secretKey = MSysConfig.getValue(
			JWTUtil.ECA52_JWT_SECRET_KEY,
			clientId
		);
		if(Util.isEmpty(secretKey, true)) {
			throw new AdempiereException(
				"@ECA52_JWT_SECRET_KEY@ @NotFound@"
			);
		}
		return secretKey;
	}
	private static SecretKey getJWT_SecretKey(int clientId) {
		byte[] keyBytes = Base64.getDecoder().decode(
			getJWT_SecretKeyAsString(clientId)
		);
		SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
		return secretKey;
	}

	/**
	 * Maximum lifetime (ms) allowed for a third party grant token. Read from SysConfig
	 * {@code ECA52_TOKEN_MAX_EXPIRE_TIME} (per client); falls back to
	 * {@link #DEFAULT_MAX_TOKEN_EXPIRE_MILLIS}. Guarantees a grant token can never be
	 * minted without an expiration nor with an unbounded one.
	 * @param clientId current client
	 * @return maximum lifetime in milliseconds (always &gt; 0)
	 */
	private static long getMaxTokenExpireMillis(int clientId) {
		String value = MSysConfig.getValue(ECA52_TOKEN_MAX_EXPIRE_TIME, clientId);
		if(!Util.isEmpty(value, true)) {
			try {
				long parsed = Long.parseLong(value.trim());
				if(parsed > 0) {
					return parsed;
				}
			} catch (NumberFormatException e) {
				//	fall through to default
			}
		}
		return DEFAULT_MAX_TOKEN_EXPIRE_MILLIS;
	}


	/**
	 * Generate the JWT and persist the {@link MADToken}.
	 * Adds the {@code token_use} and {@code scope} claims (resolved from the token
	 * profile) and snapshots the profile/scope on the token record.
	 * @param userId
	 * @param roleId
	 * @param tokenProfileId profile used to expand scopes
	 * @param tokenClaimSetId optional claim set whose active items are added as custom claims
	 * @return signed JWT value
	 */
	@Override
	public String generateToken(int userId, int roleId, int tokenProfileId, int tokenClaimSetId) {
		//	Validate user
		if(userId < 0) {
			throw new AdempiereException("@AD_User_ID@ @NotFound@");
		}
		//	Validate Role
		if(roleId < 0) {
			throw new AdempiereException("@AD_Role_ID@ @NotFound@");
		}
		MADToken token = new MADToken(Env.getCtx(), 0, null);
        token.setTokenType(MADTokenDefinition.TOKENTYPE_ThirdPartyAccess);
        if(token.getAD_TokenDefinition_ID() <= 0) {
			// TODO: Change message translation
        	throw new AdempiereException("@AD_TokenDefinition_ID@ @NotFound@");
        }

		MADTokenDefinition definition = MADTokenDefinition.getById(Env.getCtx(), token.getAD_TokenDefinition_ID(), null);
		if (definition == null || definition.getAD_TokenDefinition_ID() <= 0) {
			throw new AdempiereException("@AD_TokenDefinition_ID@ @NotFound@");
		}
		//	Resolve scopes for third party grant tokens
		String scopeString = resolveScope(tokenProfileId, definition);
		token.setAD_User_ID(userId);
		token.setAD_Role_ID(roleId);
		token.setAD_TokenProfile_ID(tokenProfileId);
		token.setScope(scopeString);
		token.saveEx();
		//	Create default session
        //	TODO: Create session with created from parameter
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		SecretKey secretKey = getJWT_SecretKey(clientId);
		long currentTimeMillis = System.currentTimeMillis();
		JwtBuilder jwtBuilder = Jwts.builder();
		//	Apply optional claim set first so custom claims cannot override reserved/system claims
		applyClaimSet(jwtBuilder, tokenClaimSetId);
		jwtBuilder
			.id(token.getUUID())
			.claim("AD_Client_ID", clientId)
			.claim("AD_Org_ID", Env.getAD_Org_ID(Env.getCtx()))
			.claim("AD_Role_ID", roleId)
			.claim("AD_User_ID", userId)
			.claim("M_Warehouse_ID", Env.getContextAsInt(Env.getCtx(), "#M_Warehouse_ID"))
			.claim("AD_Language", Env.getAD_Language(Env.getCtx()))
		;
		jwtBuilder
			.claim("token_use", "grant")
			.claim("scope", scopeString)
			.issuedAt(
				new Date(currentTimeMillis)
			)
			.signWith(secretKey, Jwts.SIG.HS256)
		;

		//	Grant tokens must always carry an exp claim and never exceed a maximum lifetime,
		//	so a third party token can never be valid forever (previously no exp when IsHasExpireDate='N').
		long maxExpireMillis = getMaxTokenExpireMillis(clientId);
		long requestedMillis;
		if(definition.isHasExpireDate()) {
			requestedMillis = Optional.ofNullable(
					definition.getExpirationTime()
				).orElse(
					new BigDecimal(DEFAULT_TOKEN_EXPIRE_MILLIS)
				).longValue()
			;
		} else {
			//	Previously non-expiring: fall back to the maximum cap.
			requestedMillis = maxExpireMillis;
		}
		//	Clamp to the cap; guard against non-positive values.
		long effectiveMillis = requestedMillis <= 0
			? maxExpireMillis
			: Math.min(requestedMillis, maxExpireMillis)
		;
		token.setExpireDate(
			new Timestamp(currentTimeMillis + effectiveMillis)
		);
		jwtBuilder.expiration(
			new Date(currentTimeMillis + effectiveMillis)
		);
		userTokenValue = jwtBuilder.compact();
		String tokenValue = null;
		try {
			//
			byte[] saltValue = new byte[8];
			// Digest computation
			tokenValue = SecureEngine.getSHA512Hash(1000, userTokenValue, saltValue);
		} catch (NoSuchAlgorithmException e) {
			new AdempiereException(e);
		} catch (UnsupportedEncodingException e) {
			new AdempiereException(e);
		}
		if(Util.isEmpty(userTokenValue)) {
			throw new AdempiereException("@TokenValue@ @NotFound@");
		}
        token.setTokenValue(tokenValue);
        token.saveEx();
        return userTokenValue;
	}

	/**
	 * Resolve the {@code scope} claim for a token profile.
	 * Respects {@link X_AD_TokenDefinition#getAD_TokenAccessType()}:
	 * Full Access ({@code F}) returns {@code "all"} without expanding the profile;
	 * Scoped ({@code S}) or null expands the profile to the space separated list of
	 * active {@link X_AD_TokenScope#getValue()} codes.
	 * @param tokenProfileId
	 * @param definition
	 * @return scope string
	 */
	private String resolveScope(int tokenProfileId, MADTokenDefinition definition) {
		//	Full access: do not expand the profile
		if(X_AD_TokenDefinition.AD_TOKENACCESSTYPE_FullAccess.equals(definition.getAD_TokenAccessType())) {
			return "all";
		}
		//	Scoped (or null): expand the profile
		if(tokenProfileId <= 0) {
			throw new AdempiereException("@AD_TokenProfile_ID@ @NotFound@");
		}
		List<X_AD_TokenScope> scopes = new Query(Env.getCtx(), I_AD_TokenScope.Table_Name,
				"EXISTS (SELECT 1 FROM AD_TokenProfileScope ps "
			  + "WHERE ps.AD_TokenScope_ID = AD_TokenScope.AD_TokenScope_ID "
			  + "AND ps.AD_TokenProfile_ID = ? AND ps.IsActive='Y')", null)
				.setParameters(tokenProfileId)
				.setOnlyActiveRecords(true)
				.<X_AD_TokenScope>list();
		if(scopes.isEmpty()) {
			throw new AdempiereException("@AD_TokenProfile_ID@ @NoScopes@");
		}
		return scopes.stream()
			.map(X_AD_TokenScope::getValue)
			.collect(Collectors.joining(" "));
	}

	/**	Reserved/system claims that a claim set must never override	*/
	private static final Set<String> RESERVED_CLAIMS = new HashSet<>(Arrays.asList(
		"AD_Client_ID", "AD_Org_ID", "AD_Role_ID", "AD_User_ID", "M_Warehouse_ID",
		"AD_Language", "token_use", "scope", "iat", "exp"
	));

	/**
	 * Add the active items of a token claim set as custom claims.
	 * Optional: when {@code tokenClaimSetId <= 0} nothing is added.
	 * Reserved/system claims are never overridden.
	 * @param jwtBuilder builder to enrich
	 * @param tokenClaimSetId claim set to expand
	 */
	private void applyClaimSet(JwtBuilder jwtBuilder, int tokenClaimSetId) {
		if(tokenClaimSetId <= 0) {
			return;
		}
		List<X_AD_TokenClaimSetItem> items = new Query(Env.getCtx(), I_AD_TokenClaimSetItem.Table_Name,
				I_AD_TokenClaimSetItem.COLUMNNAME_AD_TokenClaimSet_ID + " = ?", null)
				.setParameters(tokenClaimSetId)
				.setOnlyActiveRecords(true)
				.<X_AD_TokenClaimSetItem>list();
		for(X_AD_TokenClaimSetItem item : items) {
			String key = item.getClaimKey();
			if(Util.isEmpty(key, true) || RESERVED_CLAIMS.contains(key)) {
				continue;
			}
			jwtBuilder.claim(key, parseClaimValue(item));
		}
	}

	/**
	 * Parse a claim value according to its {@link X_AD_TokenClaimSetItem#getAD_ClaimValueType()}.
	 * @param item claim set item
	 * @return value typed as String / Number / Boolean / parsed JSON
	 */
	private Object parseClaimValue(X_AD_TokenClaimSetItem item) {
		String raw = item.getClaimValue();
		String type = item.getAD_ClaimValueType();
		if(raw == null) {
			return null;
		}
		if(X_AD_TokenClaimSetItem.AD_CLAIMVALUETYPE_Number.equals(type)) {
			return new BigDecimal(raw.trim());
		}
		if(X_AD_TokenClaimSetItem.AD_CLAIMVALUETYPE_Boolean.equals(type)) {
			return Boolean.parseBoolean(raw.trim());
		}
		if(X_AD_TokenClaimSetItem.AD_CLAIMVALUETYPE_JSON.equals(type)) {
			try {
				return new ObjectMapper().readValue(raw, Object.class);
			} catch (Exception e) {
				throw new AdempiereException(e);
			}
		}
		return raw;
	}

	@Override
	public boolean validateToken(String tokenValue) {
		String encryptedValue = null;
		try {
			Base64.Decoder decoder = Base64.getUrlDecoder();
			String[] chunks = tokenValue.split("\\.");
			if(chunks == null || chunks.length < 3) {
				throw new AdempiereException("@TokenValue@ @NotFound@");
			}
			String payload = new String(decoder.decode(chunks[1]));
			ObjectMapper mapper = new ObjectMapper();
			@SuppressWarnings("unchecked")
			Map<String, Object> dataAsMap = mapper.readValue(payload, Map.class);
			Object clientAsObject = dataAsMap.get("AD_Client_ID");
			if(clientAsObject == null
					|| !clientAsObject.getClass().isAssignableFrom(Integer.class)) {
				throw new AdempiereException("@Invalid@ @AD_Client_ID@");
			}

			SecretKey secretKey = getJWT_SecretKey(
				(int) clientAsObject
			);
			//	Validate if is token based
			JwtParser parser = Jwts.parser()
				.verifyWith(secretKey)
				.build()
			;
			Jws<Claims> claims = parser.parseSignedClaims(tokenValue);
			if(!Util.isEmpty(claims.getPayload().getId(), true)) {
				sessionId = Integer.parseInt(
					claims.getPayload().getId()
				);
			} else {
				sessionId = 0;
			}

			byte[] saltValue = new byte[8];
			encryptedValue = SecureEngine.getSHA512Hash(1000, tokenValue, saltValue);
		} catch (Exception e) {
			new AdempiereException(e);
		}
		token = getToken(encryptedValue);
		if(token == null
				|| token.getAD_Token_ID() <= 0) {
			return false;
		}
		MADTokenDefinition definition = MADTokenDefinition.getById(Env.getCtx(), token.getAD_TokenDefinition_ID(), null);
		if(definition.isHasExpireDate()) {
			Timestamp current = new Timestamp(System.currentTimeMillis());
			if(token != null && token.getExpireDate().compareTo(current) > 0) {
				return true;
			} else {
				return false;
			}
		}
		//	Is Ok
		return true;
	}
	
	/**
	 * Get system token based on encrypted user token
	 * @param encryptedValue
	 * @return
	 */
	private MADToken getToken(String encryptedValue) {
		return new Query(Env.getCtx(), MADToken.Table_Name, "TokenValue = ? "
				+ "AND EXISTS(SELECT 1 FROM AD_User_Roles ur WHERE ur.AD_User_ID = AD_Token.AD_User_ID AND ur.AD_Role_ID = AD_Token.AD_Role_ID AND ur.IsActive = 'Y') "
				+ "AND EXISTS(SELECT 1 FROM AD_User u WHERE u.AD_User_ID = AD_Token.AD_User_ID AND u.IsActive = 'Y' AND u.IsLoginUser = 'Y') "
				+ "AND EXISTS(SELECT 1 FROM AD_Role r WHERE r.AD_Role_ID = AD_Token.AD_Role_ID AND r.IsActive = 'Y')", null)
				.setParameters(encryptedValue)
				.setOnlyActiveRecords(true)
				.first();
	}

	public String getLanguage() {
		return language;
	}
	
	public int getSessionId() {
		return sessionId;
	}
}
