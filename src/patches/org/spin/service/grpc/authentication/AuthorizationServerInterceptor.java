/************************************************************************************
 * Copyright (C) 2012-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.service.grpc.authentication;

import io.grpc.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AuthorizationServerInterceptor implements ServerInterceptor {

	/**	Threaded key for context management	*/
	public static final Context.Key<Object> SESSION_CONTEXT = Context.key("session_context");
	/**	Original token (without Bearer prefix) for downstream services	*/
	public static final Context.Key<String> ORIGINAL_TOKEN = Context.key("original_token");

	/** Services/Methods allow request without Bearer token validation */
	private List<String> ALLOW_REQUESTS_WITHOUT_TOKEN = new ArrayList<String>();

	public void setAllowRequestsWithoutToken(List<String> allowRequestsWithoutToken) {
		this.ALLOW_REQUESTS_WITHOUT_TOKEN = allowRequestsWithoutToken;
	}

	public void addAllowRequestWithoutToken(String request) {
		this.ALLOW_REQUESTS_WITHOUT_TOKEN.add(request);
	}

	public List<String> getAllowRequestsWithoutToken() {
		return this.ALLOW_REQUESTS_WITHOUT_TOKEN;
	}


	/**	Revoke session	*/
	private List<String> REVOKE_TOKEN_SERVICES = new ArrayList<String>();

	public void setRevokeTokenServices(List<String> allowRequestsWithoutToken) {
		this.REVOKE_TOKEN_SERVICES = allowRequestsWithoutToken;
	}

	public void addRevokeTokenService(String request) {
		if (this.REVOKE_TOKEN_SERVICES == null) {
			this.ALLOW_REQUESTS_WITHOUT_TOKEN = new ArrayList<String>();
		}
		this.REVOKE_TOKEN_SERVICES.add(request);
	}

	public List<String> getRevokeTokenServices() {
		return this.REVOKE_TOKEN_SERVICES;
	}



	@Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
		String callingMethod = serverCall.getMethodDescriptor().getFullMethodName();
		// Bypass to ingore Bearer validation
		if (this.ALLOW_REQUESTS_WITHOUT_TOKEN.contains(callingMethod)) {
			return Contexts.interceptCall(Context.current(), serverCall, metadata, serverCallHandler);
		}

		Status status;
		String validToken = metadata.get(TokenManager.AUTHORIZATION_METADATA_KEY);
		if (validToken == null || validToken.trim().length() <= 0) {
            status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
        } else if (!validToken.startsWith(TokenManager.BEARER_TYPE)) {
            status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
        } else {
            try {
				// Detect token type: LOCAL (ADempiere HMAC) vs KEYCLOAK (pre-validated by nginx)
				String tokenWithoutBearer = TokenManager.getTokenWithoutType(validToken);
				TokenTypeDetector.TokenType tokenType = TokenTypeDetector.detect(tokenWithoutBearer);

				Properties sessioncontext;
				if (tokenType == TokenTypeDetector.TokenType.KEYCLOAK) {
					// Keycloak: decode JWT claims and resolve ADempiere session
					sessioncontext = KeycloakSessionHandler.resolveSession(tokenWithoutBearer);
				} else {
					// Local: validate JWT signature with HMAC secret key
					sessioncontext = SessionManager.getSessionFromToken(validToken);
					// Revoke session only for local tokens (Keycloak sessions are managed externally)
					if (this.REVOKE_TOKEN_SERVICES.contains(callingMethod)) {
						SessionManager.revokeSession(validToken);
					}
				}

            	Context context = Context.current()
					.withValue(SESSION_CONTEXT, sessioncontext)
					.withValue(ORIGINAL_TOKEN, tokenWithoutBearer);
                return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
            } catch (Exception e) {
                status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
				e.printStackTrace();
            }
        }

        serverCall.close(status, metadata);
        return new ServerCall.Listener<>() {
            // noop
        };
    }
}
