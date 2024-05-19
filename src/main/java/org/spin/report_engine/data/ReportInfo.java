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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.spin.report_engine.format.PrintFormat;
import org.spin.report_engine.format.PrintFormatItem;


/**
 * Call Report information is here, name, columns and other
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ReportInfo {
	private String name;
	private String description;
	private List<ColumnInfo> columns;
	private List<Row> rows;
	private Row temporaryRow;
	private int printFormatId;
	private int reportViewId;
	private boolean isSummary;
	private SummaryHandler summaryHandler;
	private List<PrintFormatItem> sortingItems;
	private int level;
	
	private ReportInfo(PrintFormat printFormat) {
		name = printFormat.getName();
		description = printFormat.getDescription();
		columns = new ArrayList<>();
		rows = new ArrayList<>();
		summaryHandler = SummaryHandler.newInstance(printFormat.getItems());
		level = printFormat.getGroupItems().size() + 1;
		sortingItems = printFormat.getSortingItems();
	}
	
	public static ReportInfo newInstance(PrintFormat printFormat) {
		return new ReportInfo(printFormat);
	}
	
	public int getLevel() {
		return level;
	}

	public ReportInfo withLevel(int minimumLevel) {
		this.level = minimumLevel;
		return this;
	}

	public String getName() {
		return name;
	}
	
	public ReportInfo withName(String name) {
		this.name = name;
		return this;
	}
	
	public String getDescription() {
		return description;
	}

	public ReportInfo withDescription(String description) {
		this.description = description;
		return this;
	}

	public List<ColumnInfo> getColumns() {
		return columns;
	}
	
	public ReportInfo withColumns(List<ColumnInfo> columns) {
		this.columns = columns;
		return this;
	}
	
	public ReportInfo addColumn(ColumnInfo column) {
		columns.add(column);
		return this;
	}
	
	public ReportInfo addRow(Row row) {
		rows.add(row);
		return this;
	}
	
	public ReportInfo addRow() {
		if(temporaryRow != null) {
			addRow(Row.newInstance().withLevel(getLevel()).withCells(temporaryRow.getData()));
			summaryHandler.addRow(Row.newInstance().withLevel(getLevel()).withCells(temporaryRow.getData()));
		}
		temporaryRow = Row.newInstance().withLevel(getLevel());
		return this;
	}
	
	public ReportInfo addCell(PrintFormatItem printFormatItem, Cell cell) {
		if(temporaryRow == null) {
			temporaryRow = Row.newInstance().withLevel(getLevel());
		}
		temporaryRow.withCell(printFormatItem.getPrintFormatItemId(), cell);
		return this;
	}
	
	public List<Row> getRows() {
		return rows;
	}

	public ReportInfo completeInfo() {
		List<Row> summaryAsRows = summaryHandler.getAsRows();
		List<Row> completeRows = Stream.concat(getRows().stream(), summaryAsRows.stream())
				.sorted(Comparator.comparing(row -> row.getSortingValue(sortingItems)))
                .collect(Collectors.toList());
		rows = completeRows;
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

	public SummaryHandler getSummaryHandler() {
		return summaryHandler;
	}

	public ReportInfo withSummaryHandler(SummaryHandler summaryHandler) {
		this.summaryHandler = summaryHandler;
		return this;
	}

	@Override
	public String toString() {
		return "ReportInfo [name=" + name + ", columns=" + columns + ", data=" + rows + ", printFormatId="
				+ printFormatId + ", reportViewId=" + reportViewId + ", isSummary=" + isSummary + "]";
	}
}
