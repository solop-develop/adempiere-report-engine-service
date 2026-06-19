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

import org.adempiere.core.domains.models.I_AD_Preference;
import org.compiere.model.MPreference;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;

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
	 * Save Session Preferences
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

		Query query = new Query(
			Env.getCtx(),
			I_AD_Preference.Table_Name,
			"AD_User_ID = ? AND Attribute = ? AND AD_Window_ID Is NULL",
			null
		)
			.setOrderBy(I_AD_Preference.COLUMNNAME_Created + " DESC")
		;

		for (String attributeName : PROPERTIES_LIST) {
			if (Util.isEmpty(attributeName, true)) {
				// next iteration
				continue;
			}
			MPreference preference = query
				.setParameters(userId, attributeName)
				.first()
			;
			if (preference == null) {
				preference = new MPreference(Env.getCtx(), 0, null);
				preference.setAD_User_ID(userId);
				preference.setAttribute(attributeName);
			} 
			if (preference.getAD_Client_ID() > 0 || preference.getAD_Org_ID() > 0) {
				preference.set_ValueOfColumn(I_AD_Preference.COLUMNNAME_AD_Client_ID, 0);
				preference.set_ValueOfColumn(I_AD_Preference.COLUMNNAME_AD_Org_ID, 0);
			}
			// set values
			if (attributeName.equals(PreferenceUtil.P_ROLE)) {
				preference.setValue(
					String.valueOf(roleId)
				);
			} else if (attributeName.equals(PreferenceUtil.P_CLIENT)) {
				preference.setValue(
					String.valueOf(clientId)
				);
			} else if (attributeName.equals(PreferenceUtil.P_ORG)) {
				preference.setValue(
					String.valueOf(organizationId)
				);
			} else if (attributeName.equals(PreferenceUtil.P_WAREHOUSE)) {
				preference.setValue(
					String.valueOf(warehouseId)
				);
			} else if (attributeName.equals(PreferenceUtil.P_LANGUAGE)) {
				String normalized = normalizeLanguageCode(language);
				if (Util.isEmpty(normalized, true)) {
					// Caller passed a value that is not a recognised
					// AD_Language code (typically the human-readable
					// display name like "Español (MX)"). Skip the save so
					// we do not overwrite the existing row with junk —
					// the next login falls back to the system default.
					continue;
				}
				preference.setValue(normalized);
			}
			preference.save();
		}
	}

}
