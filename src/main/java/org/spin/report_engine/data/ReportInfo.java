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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.spin.report_engine.format.PrintFormat;
import org.spin.report_engine.format.PrintFormatItem;
import org.spin.report_engine.format.QueryDefinition;


/**
 * Call Report information is here, name, columns and other
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ReportInfo {
	private String name;
	private String description;
	private List<ColumnInfo> columns;
	private List<Row> rows;
	private List<Row> summaryRows;
	private List<Row> groupedRows;
	private Row temporaryRow;
	private int printFormatId;
	private int reportViewId;
	private boolean isSummary;
	private SummaryHandler summaryHandler;
	private List<PrintFormatItem> sortingItems;
	private int level;
	private Map<Integer, PrintFormatItem> groupLevels;
	private QueryDefinition queryDefinition;
	private long recordCount;
	
	private ReportInfo(PrintFormat printFormat, QueryDefinition queryDefinition) {
		name = printFormat.getName();
		description = printFormat.getDescription();
		columns = printFormat.getPrintedItems().stream().map(item -> ColumnInfo.newInstance(item)).collect(Collectors.toList());
		rows = new ArrayList<Row>();
		summaryRows = new ArrayList<Row>();
		groupedRows = new ArrayList<Row>();
		summaryHandler = SummaryHandler.newInstance(printFormat.getItems());
		level = printFormat.getGroupItems().stream().mapToInt(item -> item.getSortSequence()).sum() + 1;
		AtomicInteger counter = new AtomicInteger();
		groupLevels = new HashMap<Integer, PrintFormatItem>();
		printFormat.getGroupItems().stream().sorted(Comparator.comparing(PrintFormatItem::getSortSequence)).forEach(group -> {
			group.withSortSequence(counter.getAndIncrement());
			groupLevels.put(counter.get(), group);
		});
		sortingItems = printFormat.getSortingItems();
		printFormatId = printFormat.getPrintFormatId();
		this.queryDefinition = queryDefinition;
	}
	
	public static ReportInfo newInstance(PrintFormat printFormat, QueryDefinition queryDefinition) {
		return new ReportInfo(printFormat, queryDefinition);
	}

	public Map<Integer, PrintFormatItem> getGroupLevels() {
		return groupLevels;
	}

	public QueryDefinition getQueryDefinition() {
		return queryDefinition;
	}

	public ReportInfo withQueryDefinition(QueryDefinition queryDefinition) {
		this.queryDefinition = queryDefinition;
		return this;
	}

	public int getLevel() {
		return level;
	}

	public ReportInfo withLevel(int minimumLevel) {
		this.level = minimumLevel;
		return this;
	}

	public List<PrintFormatItem> getSortingItems() {
		return sortingItems;
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
	
	public List<Row> getSummaryRows() {
		return summaryRows;
	}

	public List<Row> getGroupedRows() {
		return groupedRows;
	}

	private Comparator<Row> getSortingValue() {
		AtomicReference<Comparator<Row>> comparator = new AtomicReference<>();
		sortingItems.forEach(printFormatItem -> {
			Comparator<Row> groupComparator = (p, o) -> p.getCompareValue(printFormatItem.getPrintFormatItemId()).compareToIgnoreCase(o.getCompareValue(printFormatItem.getPrintFormatItemId()));
			if(comparator.get() == null) {
				comparator.set(groupComparator);
			} else {
				comparator.getAndUpdate(value -> value.thenComparing(groupComparator));
			}
		});
		comparator.getAndUpdate(value -> value.thenComparing(Comparator.comparing(Row::getLevel)));
		return comparator.get();
	}
	
	public ReportInfo completeInfo() {
		recordCount = rows.size();
		groupedRows = summaryHandler.getAsRows();
		List<Row> completeRows = Stream.concat(getRows().stream(), groupedRows.stream())
				.sorted(getSortingValue())
                .collect(Collectors.toList());
		rows = completeRows;
		return this;
	}
	
	/**
	 * Get all rows as tree
	 * @return
	 */
	public List<Row> getRowsAsTree() {
		PrintFormatItem levelGroup = groupLevels.get(0);
		if(levelGroup != null) {
			List<Row> tree = new ArrayList<Row>();
			//	Add parent level
			rows.stream().filter(row -> {
				return row.getLevel() == levelGroup.getSortSequence();
			}).forEach(row -> tree.add(row));
			//	Add level 1
			tree.forEach(treeValue -> {
				processChildren(treeValue, 1);
			});
			return tree;
		}
		return rows;
	}
	
	private void processChildren(Row parent, int levelAsInt) {
		List<Row> children = parent.getChildren();
		PrintFormatItem previosLevelGroup = groupLevels.get(levelAsInt - 1);
		PrintFormatItem levelGroup = groupLevels.get(levelAsInt);
		if(levelGroup == null || previosLevelGroup == null) {
			return;
		}
		rows.stream().filter(row -> {
			return row.getLevel() == levelGroup.getSortSequence() && compareRows(parent, row, levelAsInt);
		}).forEach(row -> children.add(row));
		int nextLevel = levelAsInt + 1;
		if(nextLevel < groupLevels.size()) {
			children.forEach(child -> processChildren(child, nextLevel));
		}
	}
	
	private boolean compareRows(Row parent, Row child, int currentLevel) {
		AtomicBoolean isMatched = new AtomicBoolean(true);
		IntStream.range(0, currentLevel).forEach(levelIndex -> {
			PrintFormatItem levelGroup = groupLevels.get(levelIndex);
			boolean isOk = parent.getCell(levelGroup.getPrintFormatItemId()).equals(child.getCell(levelGroup.getPrintFormatItemId()));
			if(!isOk) {
				isMatched.set(false);
			}
		});
		return isMatched.get();
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
	
	public long getRecordCount() {
		return recordCount;
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
