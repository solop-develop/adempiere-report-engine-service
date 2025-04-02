/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.report_engine.util;

import java.util.Arrays;
import java.util.List;

import org.adempiere.core.domains.models.X_AD_Table;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.compiere.util.Util;

public class RecordUtil {

	/** Table Allows Records with Zero Identifier */
	public static final List<String> ALLOW_ZERO_ID = Arrays.asList(
		X_AD_Table.ACCESSLEVEL_All,
		X_AD_Table.ACCESSLEVEL_SystemPlusClient,
		X_AD_Table.ACCESSLEVEL_ClientPlusOrganization
	);


	/**
	 * Validate tableName and MTable, and get instance
	 * @param tableName
	 * @return
	 */
	public static MTable validateAndGetTable(String tableName) {
		if (Util.isEmpty(tableName, true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		return table;
	}
	/**
	 * Validate tableName and MTable, and get instance
	 * @param tableId
	 * @return
	 */
	public static MTable validateAndGetTable(int tableId) {
		if (tableId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		MTable table = MTable.get(Env.getCtx(), tableId);
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		return table;
	}



	/**
	 * Evaluate if is valid identifier
	 * @param id
	 * @param table
	 * @return
	 */
	public static boolean isValidId(int id, MTable table) {
		if (table == null || table.getAD_Table_ID() <= 0) {
			return false;
		}

		return isValidId(
			id,
			table.getAccessLevel()
		);
	}
	/**
	 * Evaluate if is valid identifier
	 * @param id
	 * @param accesLevel
	 * @return
	 */
	public static boolean isValidId(int id, String accesLevel) {
		if (id < 0) {
			return false;
		}

		if (id == 0 && !ALLOW_ZERO_ID.contains(accesLevel)) {
			return false;
		}

		return true;
	}


	/**
	 * Evaluate if is valid identifier
	 * @param id
	 * @param accesLevel
	 * @return
	 */
	public static boolean validateRecordId(int id, String accesLevel) {
		if (!isValidId(id, accesLevel)) {
			throw new AdempiereException("@FillMandatory@ @Record_ID@ / @UUID@");
		}
		return true;
	}

}
