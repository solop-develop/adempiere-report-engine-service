/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
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
package org.spin.report_engine.format;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.compiere.util.DB;

/**
 * A stub class for reference of column to table
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 *
 */
public class ColumnReference {
	/** Table Name		*/
	private String tableName;
	/** Key Column		*/
	private String keyColumn;
	/** Display Column	*/
	private String displayColumn;
	/** Displayed		*/
	private boolean isValueDisplayed = false;
	/** Translated		*/
	private boolean	isTranslated = false;
	
	public static ColumnReference newInstance() {
		return new ColumnReference();
	}
	
	public String getTableName() {
		return tableName;
	}

	public String getKeyColumn() {
		return keyColumn;
	}

	public String getDisplayColumn() {
		return displayColumn;
	}

	public boolean isIsValueDisplayed() {
		return isValueDisplayed;
	}

	public boolean isIsTranslated() {
		return isTranslated;
	}

	public ColumnReference withTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public ColumnReference withKeyColumn(String keyColumn) {
		this.keyColumn = keyColumn;
		return this;
	}

	public ColumnReference withDisplayColumn(String displayColumn) {
		this.displayColumn = displayColumn;
		return this;
	}

	public ColumnReference withIsValueDisplayed(boolean isValueDisplayed) {
		this.isValueDisplayed = isValueDisplayed;
		return this;
	}

	public ColumnReference withIsTranslated(boolean isTranslated) {
		this.isTranslated = isTranslated;
		return this;
	}

	/**
	 *	Get TableName and ColumnName for Reference Tables.
	 *  @param referenceValueId reference value
	 *	@return 0=TableName, 1=KeyColumn, 2=DisplayColumn
	 */
	public static ColumnReference getColumnReference (int referenceValueId) {
		if (referenceValueId <= 0)
			throw new IllegalArgumentException("AD_Reference_Value_ID <= 0");
		//
		ColumnReference columnReference = ColumnReference.newInstance();
		//
		String SQL = "SELECT t.TableName, ck.ColumnName AS KeyColumn,"	//	1..2
			+ " cd.ColumnName AS DisplayColumn, rt.IsValueDisplayed, cd.IsTranslated "
			+ "FROM AD_Ref_Table rt"
			+ " INNER JOIN AD_Table t ON (rt.AD_Table_ID = t.AD_Table_ID)"
			+ " INNER JOIN AD_Column ck ON (rt.AD_Key = ck.AD_Column_ID)"
			+ " INNER JOIN AD_Column cd ON (rt.AD_Display = cd.AD_Column_ID) "
			+ "WHERE rt.AD_Reference_ID=?"			//	1
			+ " AND rt.IsActive = 'Y' AND t.IsActive = 'Y'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(SQL, null);
			pstmt.setInt (1, referenceValueId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				columnReference.withTableName(rs.getString("TableName"))
					.withKeyColumn(rs.getString("KeyColumn"))
					.withDisplayColumn(rs.getString("DisplayColumn"))
					.withIsValueDisplayed("Y".equals(rs.getString("IsValueDisplayed")))
					.withIsTranslated("Y".equals(rs.getString("IsTranslated")));
			}
		} catch (SQLException ex) {
			// None
		} finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		return columnReference;
	}	//  getTableReference
}
