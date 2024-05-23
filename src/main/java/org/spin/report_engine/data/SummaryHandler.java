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
import java.util.stream.Collectors;

import org.spin.report_engine.format.PrintFormatItem;

/**
 * This class have all need for manage row summary and groups
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class SummaryHandler {
	private List<PrintFormatItem> groupedItems;
	private List<PrintFormatItem> summarizedItems;
	private Map<Integer, Map<Row, Map<Integer, SummaryFunction>>> summary;
	private Map<Integer, Map<String, Map<Integer, SummaryFunction>>> completeSummary;
	
	private SummaryHandler(List<PrintFormatItem> printFormatItems) {
		groupedItems = printFormatItems.stream().filter(item -> item.isGroupBy()).sorted(Comparator.comparing(PrintFormatItem::getSortSequence)).collect(Collectors.toList());
		summarizedItems = printFormatItems.stream().filter(printItem -> {
			if(printItem.isHideGrandTotal()) {
				return false;
			}
			return printItem.isAveraged() || printItem.isCounted() || printItem.isMaxCalc() || printItem.isMinCalc() || printItem.isSummarized() || printItem.isVarianceCalc();
		}).collect(Collectors.toList());
		summary = new HashMap<Integer, Map<Row, Map<Integer, SummaryFunction>>>();
		completeSummary = new HashMap<Integer, Map<String, Map<Integer, SummaryFunction>>>();
	}
	
	public SummaryHandler addRow(Row row) {
		groupedItems.forEach(groupItem -> {
			Row keyRow = Row.newInstance().withLevel(groupItem.getSortSequence());
			groupedItems.stream().filter(item -> item.getSortSequence() <= groupItem.getSortSequence()).forEach(item ->{
				keyRow.withCell(item.getPrintFormatItemId(), row.getCell(item.getPrintFormatItemId()));
			});
			Map<Row, Map<Integer, SummaryFunction>> groupTotals = Optional.ofNullable(summary.get(groupItem.getPrintFormatItemId())).orElse(new HashMap<Row, Map<Integer, SummaryFunction>>());
			Map<String, Map<Integer, SummaryFunction>> totals = Optional.ofNullable(completeSummary.get(groupItem.getPrintFormatItemId())).orElse(new HashMap<String, Map<Integer,SummaryFunction>>());
			Map<Integer, SummaryFunction> columnTotals = Optional.ofNullable(groupTotals.get(keyRow)).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> sumTotals = Optional.ofNullable(totals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_SUM))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> averageTotals = Optional.ofNullable(totals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MEAN))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> countTotals = Optional.ofNullable(totals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_COUNT))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> minimumTotals = Optional.ofNullable(totals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MIN))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> maximumTotals = Optional.ofNullable(totals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MAX))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> varianceTotals = Optional.ofNullable(totals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_VARIANCE))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> deviationTotals = Optional.ofNullable(totals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_DEVIATION))).orElse(new HashMap<Integer, SummaryFunction>());
			summarizedItems.forEach(sumItem -> {
				addValue(sumItem.getPrintFormatItemId(), columnTotals, row.getCell(sumItem.getPrintFormatItemId()));
				addValue(sumItem.getPrintFormatItemId(), sumTotals, row.getCell(sumItem.getPrintFormatItemId()));
				addValue(sumItem.getPrintFormatItemId(), averageTotals, row.getCell(sumItem.getPrintFormatItemId()));
				addValue(sumItem.getPrintFormatItemId(), countTotals, row.getCell(sumItem.getPrintFormatItemId()));
				addValue(sumItem.getPrintFormatItemId(), minimumTotals, row.getCell(sumItem.getPrintFormatItemId()));
				addValue(sumItem.getPrintFormatItemId(), maximumTotals, row.getCell(sumItem.getPrintFormatItemId()));
				addValue(sumItem.getPrintFormatItemId(), varianceTotals, row.getCell(sumItem.getPrintFormatItemId()));
				addValue(sumItem.getPrintFormatItemId(), deviationTotals, row.getCell(sumItem.getPrintFormatItemId()));
			});
			groupTotals.put(keyRow, columnTotals);
			totals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_SUM), sumTotals);
			totals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MEAN), averageTotals);
			totals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_COUNT), countTotals);
			totals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MIN), minimumTotals);
			totals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MAX), maximumTotals);
			totals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_VARIANCE), varianceTotals);
			totals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_DEVIATION), deviationTotals);
			summary.put(groupItem.getPrintFormatItemId(), groupTotals);
			completeSummary.put(groupItem.getPrintFormatItemId(), totals);
		});
		return this;
	}
	
	private void addValue(int key, Map<Integer, SummaryFunction> columnTotals, Cell cell) {
		SummaryFunction function = Optional.ofNullable(columnTotals.get(key)).orElse(SummaryFunction.newInstance());
		function.addValue(cell.getFunctionValue());
		columnTotals.put(key, function);
	}
	
	public static SummaryHandler newInstance(List<PrintFormatItem> groupedItems) {
		return new SummaryHandler(groupedItems);
	}

	public List<PrintFormatItem> getGroupedItems() {
		return groupedItems;
	}

	public List<PrintFormatItem> getSummarizedItems() {
		return summarizedItems;
	}

	public Map<Integer, Map<Row, Map<Integer, SummaryFunction>>> getSummary() {
		return summary;
	}
	
	public List<Row> getAsRows() {
		List<Row> rows = new ArrayList<Row>();
		groupedItems.forEach(groupItem -> {
			Map<Row, Map<Integer, SummaryFunction>> groupTotals = Optional.ofNullable(summary.get(groupItem.getPrintFormatItemId())).orElse(new HashMap<Row, Map<Integer,SummaryFunction>>());
			groupTotals.keySet().forEach(groupValueRow -> {
//				System.err.println(groupValueRow);
				Map<Integer, SummaryFunction> summaryValue = groupTotals.get(groupValueRow);
//				Row row = Row.newInstance().withLevel(groupItem.getSortSequence()).withCell(groupItem.getPrintFormatItemId(), Cell.newInstance().withValue(groupValueRow));
				summarizedItems.forEach(sumItem -> {
					SummaryFunction function = summaryValue.get(sumItem.getPrintFormatItemId());
					groupValueRow.withCell(sumItem.getPrintFormatItemId(), Cell.newInstance().withValue(function.getValue(SummaryFunction.F_SUM)).withFunction(function));
				});
				rows.add(groupValueRow);
			});
		});
		return rows;
	}
	
//	public List<Row> getTotalsAsRows() {
//		List<Row> rows = new ArrayList<Row>();
//		groupedItems.forEach(groupItem -> {
//			Map<String, Map<Integer, SummaryFunction>> groupTotals = Optional.ofNullable(completeSummary.get(groupItem.getPrintFormatItemId())).orElse(new HashMap<String, Map<Integer,SummaryFunction>>());
//			groupTotals.keySet().forEach(groupValue -> {
//				Map<Integer, SummaryFunction> summaryValue = groupTotals.get(groupValue);
//				Row summaryRow = Row.newInstance().withLevel(0).withCell(groupItem.getPrintFormatItemId(), Cell.newInstance().withValue(groupValue));
//				summarizedItems.forEach(sumItem -> {
//					SummaryFunction function = summaryValue.get(sumItem.getPrintFormatItemId());
//					summaryRow.withCell(sumItem.getPrintFormatItemId(), Cell.newInstance().withValue(function.getValue(SummaryFunction.F_SUM)).withFunction(function));
//				});
//				rows.add(summaryRow);
//			});
//		});
//		return rows;
//	}

	@Override
	public String toString() {
		return "SummaryHandler [groupedItems=" + groupedItems + ", summarizedItems=" + summarizedItems + ", summary="
				+ summary + ", completeSummary=" + completeSummary + "]";
	}
}
