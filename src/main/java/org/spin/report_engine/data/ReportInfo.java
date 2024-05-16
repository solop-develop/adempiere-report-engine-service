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
package org.spin.report_engine.data;

import java.util.ArrayList;
import java.util.List;


/**
 * CAll Report information is here, name, columns and other
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ReportInfo {
	private String name;
	private List<ColumnInfo> columns;
	private List<Row> data;
	private int printFormatId;
	private int reportViewId;
	private boolean isSummary;
	
	private ReportInfo() {
		columns = new ArrayList<>();
		data = new ArrayList<>();
	}
	
	public static ReportInfo newInstance() {
		return new ReportInfo();
	}
	
	public String getName() {
		return name;
	}
	
	public ReportInfo withName(String name) {
		this.name = name;
		return this;
	}
	
	public List<ColumnInfo> getColumns() {
		return columns;
	}
	
	public ReportInfo withColumns(List<ColumnInfo> columns) {
		this.columns = columns;
		return this;
	}
	
	public ReportInfo addRow(Row row) {
		data.add(row);
		return this;
	}
	
	public int getPrintFormatId() {
		return printFormatId;
	}

	public ReportInfo withPrintFormatId(int printFormatId) {
		this.printFormatId = printFormatId;
		return this;
	}

	public int getReportViewId() {
		return reportViewId;
	}

	public ReportInfo withReportViewId(int reportViewId) {
		this.reportViewId = reportViewId;
		return this;
	}

	public boolean isSummary() {
		return isSummary;
	}

	public ReportInfo withSummary(boolean isSummary) {
		this.isSummary = isSummary;
		return this;
	}

	@Override
	public String toString() {
		return "ReportInfo [name=" + name + ", columns=" + columns + ", data=" + data + ", printFormatId="
				+ printFormatId + ", reportViewId=" + reportViewId + ", isSummary=" + isSummary + "]";
	}
}
