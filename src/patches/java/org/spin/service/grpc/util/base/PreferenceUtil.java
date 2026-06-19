/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt EdwinBetanc0urt@outlook.com                     *
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
package org.spin.service.grpc.util.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.adempiere.core.domains.models.I_AD_Preference;
import org.compiere.model.MPreference;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.service.grpc.util.value.NumberManager;

public class PreferenceUtil {
	/** Language			*/
	public static final String P_LANGUAGE = "Language";

	/** Role */
	public static final String P_ROLE = "Role";

	/** Client Name */
	public static final String P_CLIENT = "Client";

	/** Org Name */
	public static final String P_ORG = "Organization";

	/** Warehouse Name */
	public static final String P_WAREHOUSE = "Warehouse";


	public static List<String> PROPERTIES_LIST = Arrays.asList(
		P_LANGUAGE, P_ROLE, P_CLIENT, P_ORG, P_WAREHOUSE
	);


	/**
	 * Get Session Preferences
	 * @param userId
	 * @return
	 */
	public static List<MPreference> getSessionPreferences(int userId) {
		List<MPreference> preferencesList = new ArrayList<MPreference>();
		if (userId <= 0) {
			return preferencesList;
		}

		ArrayList<Object> queryParameters = new ArrayList<>();
		queryParameters.add(userId);
		queryParameters.addAll(PreferenceUtil.PROPERTIES_LIST);

		preferencesList = new Query(
			Env.getCtx(),
			I_AD_Preference.Table_Name,
			"AD_User_ID = ? AND Attribute IN(?, ?, ?, ?, ?) AND AD_Window_ID Is NULL",
			null
		)
			.setOrderBy(I_AD_Preference.COLUMNNAME_Created + " DESC")
			.setParameters(queryParameters)
			.<MPreference>list()
		;
		return preferencesList;
	}


	/**
	 * Get the most recent value of a single preference for a user.
	 * Returns null if the preference is not set or invalid input.
	 *
	 * @param userId AD_User_ID owner of the preference
	 * @param attributeName one of the P_* constants in this class
	 * @return raw string value of the preference, or null
	 */
	public static String getPreferenceValue(int userId, String attributeName) {
		if (userId <= 0 || Util.isEmpty(attributeName, true)) {
			return null;
		}
		MPreference preference = new Query(
			Env.getCtx(),
			I_AD_Preference.Table_Name,
			"AD_User_ID = ? AND Attribute = ? AND AD_Window_ID Is NULL",
			null
		)
			.setOrderBy(I_AD_Preference.COLUMNNAME_Created + " DESC")
			.setParameters(userId, attributeName)
			.<MPreference>first()
		;
		if (preference == null) {
			return null;
		}
		return preference.getValue();
	}


	/**
	 * Parse a preference value into a positive AD_*_ID, or return -1 when the
	 * value is missing or non-numeric. Callers use the -1 sentinel to fall back
	 * to a system default and to avoid trusting unparseable preference data.
	 */
	private static int parseIdOrNegative(String value) {
		if (Util.isEmpty(value, true)) {
			return -1;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return -1;
		}
	}


	/**
	 * Get the user's preferred language as a valid {@code AD_Language} code
	 * (e.g. {@code es_VE}). Returns null when the preference is missing,
	 * blank, or when the stored value does not normalise to any row in
	 * {@code AD_Language} — this last case protects against legacy data
	 * where the human-readable name (e.g. "Español (MX)") was saved into
	 * {@code AD_Preference.Value} instead of the language code, which
	 * downstream services then forward as a query parameter and break.
	 *
	 * 2-char ISO codes (e.g. {@code "es"}, {@code "en"}) are accepted and
	 * resolved to the matching system/base AD_Language.
	 */
	public static String getLanguagePreference(int userId) {
		return normalizeLanguageCode(
			getPreferenceValue(userId, P_LANGUAGE)
		);
	}


	/**
	 * Thin wrapper around {@link MPreference#normalizeLanguageCode(String)}
	 * so the tiered lookup against {@code AD_Language} lives in a single
	 * place. Returns the canonical {@code AD_Language} code, or null when
	 * the input does not match any row.
	 */
	private static String normalizeLanguageCode(String input) {
		return MPreference.normalizeLanguageCode(input);
	}


	/**
	 * Get the user's preferred AD_Role_ID, or -1 if no preference is set.
	 * Callers MUST validate that the user still has access to this role
	 * before using it (privilege-escalation defense).
	 */
	public static int getRolePreference(int userId) {
		return parseIdOrNegative(
			getPreferenceValue(userId, P_ROLE)
		);
	}


	/**
	 * Get the user's preferred AD_Client_ID, or -1 if no preference is set.
	 * Normally derived from the preferred role; kept for traceability.
	 */
	public static int getClientPreference(int userId) {
		return parseIdOrNegative(
			getPreferenceValue(userId, P_CLIENT)
		);
	}


	/**
	 * Get the user's preferred AD_Org_ID, or -1 if no preference is set.
	 * Callers MUST validate that the (user, role) combination still has
	 * access to this organization (privilege-escalation defense).
	 */
	public static int getOrganizationPreference(int userId) {
		return parseIdOrNegative(
			getPreferenceValue(userId, P_ORG)
		);
	}


	/**
	 * Get the user's preferred M_Warehouse_ID, or -1 if no preference is set.
	 */
	public static int getWarehousePreference(int userId) {
		return parseIdOrNegative(
			getPreferenceValue(userId, P_WAREHOUSE)
		);
	}


	/**
	 * Persist one session-level preference for the user, creating the row
	 * when it does not exist yet. Client/Org are normalised to 0 (system)
	 * so the value is treated as a user-global preference, matching the
	 * lookup predicates used elsewhere in this class.
	 *
	 * The language attribute is rejected when the supplied value does not
	 * resolve to any {@code AD_Language} row — typically the human-readable
	 * display name like {@code "Español (MX)"}. In that case the existing
	 * row (if any) is left untouched and {@code null} is returned, so the
	 * caller does not assume a successful write. This is in addition to
	 * the defence-in-depth normalisation in {@link MPreference#beforeSave}.
	 *
	 * @param userId AD_User_ID owner of the preference
	 * @param attributeName one of the P_* constants in this class
	 * @param value raw value to persist
	 * @return the persisted preference, or {@code null} when input is
	 *         invalid or the language could not be normalised
	 */
	public static MPreference saveSessionPreference(int userId, String attributeName, String value) {
		if (userId <= 0 || Util.isEmpty(attributeName, true)) {
			return null;
		}
		// Refuse attributes that are not part of the known session-level
		// set so this method cannot be hijacked to write arbitrary
		// preference rows on behalf of the user.
		if (!PROPERTIES_LIST.contains(attributeName)) {
			return null;
		}
		String storedValue = value;
		if (P_LANGUAGE.equals(attributeName)) {
			storedValue = normalizeLanguageCode(value);
			if (Util.isEmpty(storedValue, true)) {
				return null;
			}
		}
		MPreference preference = new Query(
			Env.getCtx(),
			I_AD_Preference.Table_Name,
			"AD_User_ID = ? AND Attribute = ? AND AD_Window_ID Is NULL",
			null
		)
			.setOrderBy(I_AD_Preference.COLUMNNAME_Created + " DESC")
			.setParameters(userId, attributeName)
			.<MPreference>first()
		;
		if (preference == null) {
			preference = new MPreference(Env.getCtx(), 0, null);
			preference.setAD_User_ID(userId);
			preference.setAttribute(attributeName);
		}
		if (preference.getAD_Client_ID() > 0) {
			preference.set_ValueOfColumn(I_AD_Preference.COLUMNNAME_AD_Client_ID, 0);
		}
		if (preference.getAD_Org_ID() > 0) {
			preference.set_ValueOfColumn(I_AD_Preference.COLUMNNAME_AD_Org_ID, 0);
		}
		preference.setValue(storedValue);
		preference.save();
		return preference;
	}


	/**
	 * Persist only the attributes present in {@code attributes}. Use this
	 * from endpoints that update a single field (warehouse-only,
	 * language-only) without re-writing the other rows with stale session
	 * values. Entries whose attribute name is blank are ignored.
	 */
	public static void saveSessionPreferences(int userId, Map<String, String> attributes) {
		if (userId <= 0 || attributes == null || attributes.isEmpty()) {
			return;
		}
		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			saveSessionPreference(userId, entry.getKey(), entry.getValue());
		}
	}


	/**
	 * Save the full set of session preferences (Language, Role, Client,
	 * Organization, Warehouse). Used by role-change flows where all five
	 * are expected to move together.
	 *
	 * @param userId
	 * @param language
	 * @param roleId
	 * @param clientId
	 * @param organizationId
	 * @param warehouseId
	 */
	public static void saveSessionPreferences(
		// query
		int userId,
		// values
		String language, int roleId, int clientId, int organizationId, int warehouseId
	) {
		if (userId <= 0) {
			return;
		}
		saveSessionPreference(userId, P_LANGUAGE, language);
		saveSessionPreference(userId, P_ROLE, NumberManager.getIntToString(roleId));
		saveSessionPreference(userId, P_CLIENT, NumberManager.getIntToString(clientId));
		saveSessionPreference(userId, P_ORG, NumberManager.getIntToString(organizationId));
		saveSessionPreference(userId, P_WAREHOUSE, NumberManager.getIntToString(warehouseId));
	}


	/** Convenience setter — persist the user's preferred language. */
	public static MPreference saveLanguagePreference(int userId, String language) {
		return saveSessionPreference(userId, P_LANGUAGE, language);
	}


	/** Convenience setter — persist the user's preferred AD_Role_ID. */
	public static MPreference saveRolePreference(int userId, int roleId) {
		return saveSessionPreference(userId, P_ROLE, NumberManager.getIntToString(roleId));
	}


	/** Convenience setter — persist the user's preferred AD_Client_ID. */
	public static MPreference saveClientPreference(int userId, int clientId) {
		return saveSessionPreference(userId, P_CLIENT, NumberManager.getIntToString(clientId));
	}


	/** Convenience setter — persist the user's preferred AD_Org_ID. */
	public static MPreference saveOrganizationPreference(int userId, int organizationId) {
		return saveSessionPreference(userId, P_ORG, NumberManager.getIntToString(organizationId));
	}


	/** Convenience setter — persist the user's preferred M_Warehouse_ID. */
	public static MPreference saveWarehousePreference(int userId, int warehouseId) {
		return saveSessionPreference(userId, P_WAREHOUSE, NumberManager.getIntToString(warehouseId));
	}

}
