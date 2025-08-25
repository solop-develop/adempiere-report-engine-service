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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.report_engine.format.PrintFormat;
import org.spin.report_engine.format.PrintFormatItem;
import org.spin.report_engine.format.QueryDefinition;
import org.spin.report_engine.mapper.DefaultMapping;
import org.spin.report_engine.mapper.IColumnMapping;
import org.spin.report_engine.util.ClassLoaderMapping;


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
	private int instanceId;
	private PrintFormat printFormat;
	private String tableName;

	private ReportInfo(PrintFormat printFormat, QueryDefinition queryDefinition) {
		this.printFormat = printFormat;
		name = printFormat.getName();
		description = printFormat.getDescription();
		columns = printFormat.getPrintedItems()
			.stream()
			.map(item -> {
				return ColumnInfo.newInstance(item);
			})
			.collect(Collectors.toList())
		;
		rows = new ArrayList<Row>();
		summaryRows = new ArrayList<Row>();
		groupedRows = new ArrayList<Row>();
		summaryHandler = SummaryHandler.newInstance(printFormat.getItems());
		level = printFormat.getGroupItems()
			.stream()
			.mapToInt(item -> {
				return item.getSortSequence();
			})
			.sum() + 1;
		AtomicInteger counter = new AtomicInteger();
		groupLevels = new HashMap<Integer, PrintFormatItem>();
		printFormat.getGroupItems()
			.stream()
			.sorted(Comparator.comparing(PrintFormatItem::getSortSequence))
			.forEach(group -> {
				group.withSortSequence(counter.get());
				groupLevels.put(counter.getAndIncrement(), group);
			})
		;
		sortingItems = printFormat.getSortingItems();
		printFormatId = printFormat.getPrintFormatId();
		this.queryDefinition = queryDefinition;
		this.tableName = printFormat.getTableName();
	}

	public static ReportInfo newInstance(PrintFormat printFormat, QueryDefinition queryDefinition) {
		return new ReportInfo(printFormat, queryDefinition);
	}

	public Map<Integer, PrintFormatItem> getGroupLevels() {
		return groupLevels;
	}

	public String getTableName() {
		return tableName;
	}

	public ReportInfo withTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public QueryDefinition getQueryDefinition() {
		return queryDefinition;
	}

	public ReportInfo withQueryDefinition(QueryDefinition queryDefinition) {
		this.queryDefinition = queryDefinition;
		return this;
	}

	public int getInstanceId() {
		return instanceId;
	}

	public ReportInfo withInstanceId(int instanceId) {
		this.instanceId = instanceId;
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
	public ReportInfo addRow(int level, int sequence) {
		if(temporaryRow != null) {
			addRow(
				Row.newInstance()
					.withLevel(level)
					.withCells(temporaryRow.getData())
					.withSequence(sequence)
			);
			summaryHandler.addRow(
				Row.newInstance()
					.withLevel(level)
					.withSummaryRow(true)
					.withCells(temporaryRow.getData())
			);
		}
		temporaryRow = Row.newInstance().withLevel(level);
		return this;
	}
	public ReportInfo addRow() {
		if(temporaryRow != null) {
			addRow(
				Row.newInstance()
					.withLevel(getLevel())
					.withCells(temporaryRow.getData())
			);
			summaryHandler.addRow(
				Row.newInstance()
					.withLevel(getLevel())
					.withSummaryRow(true)
					.withCells(temporaryRow.getData())
			);
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

	public List<Row> getCompleteRows() {
		return rows.stream()
			.sorted(getSortingValue(true))
			.collect(Collectors.toList())
		;
	}

	public List<Row> getSummaryRows() {
		return summaryRows;
	}

	public List<Row> getGroupedRows() {
		return groupedRows;
	}

	private Comparator<Row> getSortingValue(boolean summaryAtEnd) {
		AtomicReference<Comparator<Row>> comparator = new AtomicReference<>();
		sortingItems.forEach(printFormatItem -> {
			Comparator<Row> groupComparator = (p, o) -> p.getCompareValue(printFormatItem.getPrintFormatItemId()).compareToIgnoreCase(o.getCompareValue(printFormatItem.getPrintFormatItemId()));
			if(comparator.get() == null) {
				comparator.set(groupComparator);
			} else {
				comparator.getAndUpdate(value -> value.thenComparing(groupComparator));
			}
		});
		if(comparator.get() != null) {
			if(!summaryAtEnd) {
				comparator.getAndUpdate(value -> value.thenComparing(Comparator.comparing(Row::getLevel)));
			} else {
				comparator.getAndUpdate(value -> value.thenComparing(Comparator.comparing(Row::getLevel).reversed()));
			}
		} else {
			if(!summaryAtEnd) {
				comparator.set(Comparator.comparing(Row::getLevel).reversed());
			} else {
				comparator.set(Comparator.comparing(Row::getLevel));
			}
		}
		return comparator.get();
	}

	public PrintFormat getPrintFormat() {
		return printFormat;
	}

	public ReportInfo completeInfo() {
		Map<Integer, Integer> columnLength = new HashMap<>();
		groupedRows = summaryHandler.getAsRows();
		List<Row> completeRows = Stream.concat(getRows().stream(), groupedRows.stream())
			.sorted(getSortingValue(false))
			.collect(Collectors.toList())
		;
		Language language = Language.getLoginLanguage();
		rows = new ArrayList<Row>();
		summaryRows = new ArrayList<Row>();
		completeRows.forEach(row -> {
			Row newRow = Row.newInstance().withSourceRowDefinition(row);
			//	Items
			printFormat.getItems().forEach(printFormatItem -> {
				Cell cell = row.getCell(printFormatItem.getPrintFormatItemId());
				//	Apply Default Mask
				if(!Util.isEmpty(printFormatItem.getMappingClassName())) {
					IColumnMapping customMapping = ClassLoaderMapping.loadClass(printFormatItem.getMappingClassName());
					if(customMapping != null) {
						customMapping.processValue(printFormatItem, language, cell);
					}
				} else {
					DefaultMapping.newInstance().processValue(printFormatItem, language, cell);
				}
				int newLength = Optional.ofNullable(cell.getDisplayValue()).orElse("").length();
				if(columnLength.containsKey(printFormatItem.getPrintFormatItemId())) {
					int characters = columnLength.get(printFormatItem.getPrintFormatItemId());
					if(newLength > characters) {
						columnLength.put(printFormatItem.getPrintFormatItemId(), newLength);
					}
				} else {
					columnLength.put(printFormatItem.getPrintFormatItemId(), newLength);
				}
				newRow.withCell(printFormatItem.getPrintFormatItemId(), cell);
			});
			rows.add(newRow);
			if(newRow.isSummaryRow() || (newRow.getLevel() == 0 && isFinancialReport())) {
				summaryRows.add(newRow);
			}
		});

		if (columnLength == null || columnLength.isEmpty()) {
			return this;
		}
		columns = columns.stream()
			.map(column -> {
				Integer columnCharactersSizeId = columnLength.get(column.getPrintFormatItemId());
				return column.withColumnCharactersSize(columnCharactersSizeId);
			})
			.collect(Collectors.toList())
		;
		return this;
	}

	private boolean isFinancialReport() {
		return getTableName().equals("T_Report");
	}

	/**
	 * Get all rows as tree
	 * @return
	 */
	public List<Row> getRowsAsTree() {
		if(isFinancialReport()) {
			List<Row> tree = new ArrayList<Row>();
			//	Add parent level
			rows.stream()
				.filter(row -> {
					return row.getLevel() == 0;
				}).forEach(row -> {
					tree.add(row);
				})
			;
			tree.forEach(treeValue -> {
				processChildrenFinancialReport(treeValue, 1);
			});
			return tree;
		} else {
			PrintFormatItem levelGroup = groupLevels.get(0);
			if(levelGroup != null) {
				List<Row> tree = new ArrayList<Row>();
				//	Add parent level
				rows.stream()
					.filter(row -> {
						return row.getLevel() == levelGroup.getSortSequence();
					})
					.forEach(row -> {
						tree.add(row);
					})
				;
				tree.forEach(treeValue -> {
					processChildren(treeValue, 1);
				});
				return tree;
			}
		}
		return rows;
	}

	private void processChildrenFinancialReport(Row parent, int levelAsInt) {
		List<Row> children = parent.getChildren();
		rows.stream()
			.filter(row -> {
				return row.getLevel() == levelAsInt 
					&& row.getSequence() == parent.getSequence()
				;
			})
			.forEach(row -> {
				children.add(row);
			})
		;
		int nextLevel = levelAsInt + 1;
		children.forEach(child -> processChildrenFinancialReport(child, nextLevel));
	}

	private void processChildren(Row parent, int levelAsInt) {
		List<Row> children = parent.getChildren();
		PrintFormatItem previosLevelGroup = groupLevels.get(levelAsInt - 1);
		PrintFormatItem levelGroup = groupLevels.get(levelAsInt);
		if((levelGroup == null && groupLevels.size() > 1) || previosLevelGroup == null) {
			return;
		}
		rows.stream()
			.filter(row -> {
				if(levelGroup == null && row.getLevel() > parent.getLevel() && compareRows(parent, row, levelAsInt)) {
					return true;
				}
				if(levelGroup != null && row.getLevel() == levelGroup.getSortSequence() && compareRows(parent, row, levelAsInt)) {
					return true;
				}
				return false;
			})
			.forEach(row -> {
				children.add(row);
			})
		;
		//	No Recursive
		if(groupLevels.size() == 1) {	
			return;
		}
		int nextLevel = levelAsInt + 1;
		if(nextLevel < groupLevels.size()) {
			children.forEach(child -> processChildren(child, nextLevel));
		} else {
			children.forEach(child -> processAllChildren(child));
		}
	}

	private void processAllChildren(Row parent) {
		List<Row> children = parent.getChildren();
		PrintFormatItem previosLevelGroup = groupLevels.get(groupLevels.size() - 1);
		if(previosLevelGroup == null) {
			return;
		}
		rows.stream()
			.filter(row -> {
				return row.getLevel() > parent.getLevel()
					&& compareRows(parent, row, groupLevels.size())
				;
			})
			.forEach(row -> {
				children.add(row);
			})
		;
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
	
	public ReportInfo withRecordCount(int recordCount) {
		this.recordCount = recordCount;
		return this;
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
		return "ReportInfo [name=" + name + ", columns=" + columns
			+ ", data=" + rows + ", printFormatId=" + printFormatId
			+ ", reportViewId=" + reportViewId + ", isSummary=" + isSummary + "]"
		;
	}

}
