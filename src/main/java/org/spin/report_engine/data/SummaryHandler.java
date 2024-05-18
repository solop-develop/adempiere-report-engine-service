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
	private Map<Integer, Map<String, Map<Integer, SummaryFunction>>> summary;
	
	private SummaryHandler(List<PrintFormatItem> printFormatItems) {
		groupedItems = printFormatItems.stream().filter(item -> item.isGroupBy()).collect(Collectors.toList());
		summarizedItems = printFormatItems.stream().filter(printItem -> {
			if(printItem.isHideGrandTotal()) {
				return false;
			}
			return printItem.isAveraged() || printItem.isCounted() || printItem.isMaxCalc() || printItem.isMinCalc() || printItem.isSummarized() || printItem.isVarianceCalc();
		}).collect(Collectors.toList());
		summary = new HashMap<Integer, Map<String, Map<Integer, SummaryFunction>>>();
	}
	
	public SummaryHandler addRow(Row row) {
		groupedItems.forEach(groupItem -> {
			Map<String, Map<Integer, SummaryFunction>> groupTotals = Optional.ofNullable(summary.get(groupItem.getPrintFormatItemId())).orElse(new HashMap<String, Map<Integer,SummaryFunction>>());
			String groupKey = row.getCell(groupItem.getPrintFormatItemId()).getDisplayValue();
			Map<Integer, SummaryFunction> columnTotals = Optional.ofNullable(groupTotals.get(groupKey)).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> sumTotals = Optional.ofNullable(groupTotals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_SUM))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> averageTotals = Optional.ofNullable(groupTotals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MEAN))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> countTotals = Optional.ofNullable(groupTotals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_COUNT))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> minimumTotals = Optional.ofNullable(groupTotals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MIN))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> maximumTotals = Optional.ofNullable(groupTotals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MAX))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> varianceTotals = Optional.ofNullable(groupTotals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_VARIANCE))).orElse(new HashMap<Integer, SummaryFunction>());
			Map<Integer, SummaryFunction> deviationTotals = Optional.ofNullable(groupTotals.get(SummaryFunction.getFunctionSymbol(SummaryFunction.F_DEVIATION))).orElse(new HashMap<Integer, SummaryFunction>());
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
			groupTotals.put(groupKey, columnTotals);
			groupTotals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_SUM), sumTotals);
			groupTotals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MEAN), averageTotals);
			groupTotals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_COUNT), countTotals);
			groupTotals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MIN), minimumTotals);
			groupTotals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_MAX), maximumTotals);
			groupTotals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_VARIANCE), varianceTotals);
			groupTotals.put(SummaryFunction.getFunctionSymbol(SummaryFunction.F_DEVIATION), deviationTotals);
			summary.put(groupItem.getPrintFormatItemId(), groupTotals);
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

	public Map<Integer, Map<String, Map<Integer, SummaryFunction>>> getSummary() {
		return summary;
	}

	@Override
	public String toString() {
		return "SummaryHandler [groupedItems=" + groupedItems + ", summarizedItems=" + summarizedItems + ", summary="
				+ summary + "]";
	}
}
