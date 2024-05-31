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

import org.compiere.model.MColumn;
import org.compiere.util.Util;

/**
 * Print Format Line Representation
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class PrintFormatColumn {
	
	private int printFormatItemId;
	//	Column attributes
	private String columnName;
	private String columnNameAlias;
	private int columnId;
	private int referenceId;
	private int referenceValueId;
	private boolean isKey;
	private boolean isParent;
	private boolean isMandatory;
	private boolean isVirtualColumn;
	private String columnSql;
	private boolean isDisplayValue;
	
	private PrintFormatColumn(MColumn column) {
		columnName = column.getColumnName();
		columnId = column.getAD_Column_ID();
		referenceId = column.getAD_Reference_ID();
		referenceValueId = column.getAD_Reference_Value_ID();
		isKey = column.isKey();
		isParent = column.isParent();
		isMandatory = column.isMandatory();
		columnSql = column.getColumnSQL();
		isVirtualColumn = !Util.isEmpty(column.getColumnSQL());
	}
	
	private PrintFormatColumn(PrintFormatItem printFormatItem) {
		printFormatItemId = printFormatItem.getPrintFormatItemId();
		columnName = printFormatItem.getColumnName();
		columnId = printFormatItem.getColumnId();
		referenceId = printFormatItem.getReferenceId();
		referenceValueId = printFormatItem.getReferenceValueId();
		isKey = printFormatItem.isKey();
		isParent = printFormatItem.isParent();
		isMandatory = printFormatItem.isMandatory();
		columnSql = printFormatItem.getColumnSql();
		isVirtualColumn = !Util.isEmpty(printFormatItem.getColumnSql());
	}
	
	public static PrintFormatColumn newInstance(PrintFormatItem printFormatItem) {
		return new PrintFormatColumn(printFormatItem);
	}
	
	public static PrintFormatColumn newInstance(MColumn column) {
		return new PrintFormatColumn(column);
	}

	public int getPrintFormatItemId() {
		return printFormatItemId;
	}

	public String getColumnName() {
		return columnName;
	}

	public int getColumnId() {
		return columnId;
	}

	public int getReferenceId() {
		return referenceId;
	}

	public int getReferenceValueId() {
		return referenceValueId;
	}

	public boolean isKey() {
		return isKey;
	}

	public boolean isParent() {
		return isParent;
	}
	
	public boolean isVirtualColumn() {
		return isVirtualColumn;
	}

	public String getColumnSql() {
		return columnSql;
	}

	public boolean isMandatory() {
		return isMandatory;
	}

	public String getColumnNameAlias() {
		return columnNameAlias;
	}
	
	public PrintFormatColumn withColumnName(String columnName) {
		this.columnName = columnName;
		return this;
	}

	public PrintFormatColumn withColumnNameAlias(String columnNameAlias) {
		this.columnNameAlias = columnNameAlias;
		return this;
	}

	public boolean isDisplayValue() {
		return isDisplayValue;
	}

	public PrintFormatColumn withDisplayValue(boolean isDisplayValue) {
		this.isDisplayValue = isDisplayValue;
		return this;
	}

	@Override
	public String toString() {
		return "PrintFormatColumn [printFormatItemId=" + printFormatItemId + ", columnName=" + columnName
				+ ", columnNameAlias=" + columnNameAlias + ", columnId=" + columnId + ", referenceId=" + referenceId
				+ ", referenceValueId=" + referenceValueId + ", isKey=" + isKey + ", isParent=" + isParent
				+ ", isMandatory=" + isMandatory + ", isVirtualColumn=" + isVirtualColumn + ", columnSql=" + columnSql
				+ ", isDisplayValue=" + isDisplayValue + "]";
	}
}
