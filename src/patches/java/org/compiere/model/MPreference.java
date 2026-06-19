/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
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
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.core.domains.models.X_AD_Preference;
import org.compiere.util.DB;
import org.compiere.util.Util;

/**
 *	Preference Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MPreference.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MPreference extends X_AD_Preference
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5098559160325123593L;
	/**	Null Indicator				*/
	public static String		NULL = "null";
	
	/**
	 * 	Standatrd Constructor
	 *	@param ctx ctx
	 *	@param AD_Preference_ID id
	 *	@param trxName transaction
	 */
	public MPreference(Properties ctx, int AD_Preference_ID, String trxName)
	{
		super(ctx, AD_Preference_ID, trxName);
		if (AD_Preference_ID == 0)
		{
		//	setAD_Preference_ID (0);
		//	setAttribute (null);
		//	setValue (null);
		}
	}	//	MPreference

	/**
	 * 	Load Contsructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MPreference(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MPreference

	/**
	 * 	Full Constructor
	 *	@param ctx context
	 *	@param Attribute attribute
	 *	@param Value value
	 *	@param trxName trx
	 */
	public MPreference (Properties ctx, String Attribute, String Value, String trxName)
	{
		this (ctx, 0, trxName);
		setAttribute (Attribute);
		setValue (Value);
	}	//	MPreference

	/** Attribute name that stores the user's preferred language. */
	private static final String ATTRIBUTE_LANGUAGE = "Language";

	/**
	 * 	Before Save
	 *	@param newRecord
	 *	@return true if can be saved
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		String value = getValue();
		if (value == null)
			value = "";
		if (value.equals("-1"))
			setValue("");
		// Defense-in-depth: any preference row holding the user's language
		// must store a valid AD_Language code (e.g. "es_MX"), never a
		// localized display name (e.g. "Español (MX)"). Legacy ZK UI
		// flows persisted the display name directly, which downstream
		// consumers then forwarded as an identifier — breaking the menu
		// API, Elasticsearch index resolution, and print engines.
		if (ATTRIBUTE_LANGUAGE.equals(getAttribute()) && !Util.isEmpty(getValue(), true)) {
			String normalized = normalizeLanguageCode(getValue());
			if (!Util.isEmpty(normalized, true)) {
				setValue(normalized);
			}
		}
		return true;
	}	//	beforeSave

	/**
	 * Resolve any language string into a canonical {@code AD_Language}
	 * code by a tiered lookup against {@code AD_Language}. The input
	 * length picks the column to match:
	 * <ol>
	 *   <li><b>2-char ISO</b> ({@code "es"}, {@code "en"}) → matched
	 *       against {@code LanguageISO}, returning the system/base
	 *       AD_Language for that ISO.</li>
	 *   <li><b>Exact AD_Language code</b> ({@code "es_MX"}, {@code
	 *       "en_US"}) → matched against {@code AD_Language}.</li>
	 *   <li><b>Anything else</b> (display names like {@code "Español
	 *       (MX)"}) → matched against {@code Name} or {@code PrintName}.</li>
	 * </ol>
	 * Returns null when no tier produces a match — the caller leaves the
	 * raw value in place so legacy data is not destroyed.
	 */
	public static String normalizeLanguageCode(String input) {
		if (Util.isEmpty(input, true)) {
			return null;
		}
		String trimmed = input.trim();
		if (trimmed.length() == 2) {
			return DB.getSQLValueString(
				null,
				"SELECT AD_Language FROM AD_Language "
					+ "WHERE UPPER(LanguageISO) = UPPER(?) "
					+ "AND (IsSystemLanguage = 'Y' OR IsBaseLanguage = 'Y') "
					+ "AND ROWNUM = 1",
				trimmed
			);
		}
		String code = DB.getSQLValueString(
			null,
			"SELECT AD_Language FROM AD_Language "
				+ "WHERE UPPER(AD_Language) = UPPER(?) "
				+ "AND ROWNUM = 1",
			trimmed
		);
		if (!Util.isEmpty(code, true)) {
			return code;
		}
		return DB.getSQLValueString(
			null,
			"SELECT AD_Language FROM AD_Language "
				+ "WHERE (UPPER(Name) = UPPER(?) OR UPPER(PrintName) = UPPER(?)) "
				+ "AND ROWNUM = 1",
			trimmed, trimmed
		);
	}

	/**
	 * 	String Representation
	 *	@return info
	 */
	public String toString ()
	{
		StringBuffer sb = new StringBuffer ("MPreference[");
		sb.append (get_ID()).append("-")
			.append(getAttribute()).append("-").append(getValue())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
}	//	MPreference
