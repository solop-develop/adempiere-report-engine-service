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
package org.spin.report_engine.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.spin.backend.grpc.report_engine.ReportColumn;
import org.spin.backend.grpc.report_engine.ReportRow;
import org.spin.backend.grpc.report_engine.GetReportRequest;
import org.spin.backend.grpc.report_engine.Report;
import org.spin.report_engine.data.Cell;
import org.spin.report_engine.data.ColumnInfo;
import org.spin.report_engine.data.ReportInfo;
import org.spin.report_engine.data.Row;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

public class Service {
	private static final String VALUE_KEY = "value";
	private static final String DISPLAY_VALUE_KEY = "display_value";
	private static final String SUM_KEY = "sum_value";
	private static final String MEAN_KEY = "mean_value";
	private static final String COUNT_KEY = "count_value";
	private static final String MINIMUM_KEY = "minimum_value";
	private static final String MAXIMUM_KEY = "maximum_value";
	private static final String VARIANCE_KEY = "variance_value";
	private static final String DEVIATION_KEY = "deviation_value";
	
	/**
	 * Run Report
	 * @param context
	 * @param request
	 * @return
	 */
	public static Report.Builder getReport(GetReportRequest request) {
		if(request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_PrintFormat_ID@");
		}
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		ReportInfo reportInfo = ReportBuilder.newInstance(request.getId())
				.withFilters(FilterManager.newInstance(request.getFilters())
				.getConditions())
				.run(limit, offset);
		//	
		Report.Builder builder = Report.newBuilder();
		builder.setName(ValueManager.validateNull(reportInfo.getName()))
		.setDescription(ValueManager.validateNull(reportInfo.getDescription()))
		.setId(reportInfo.getPrintFormatId())
		.setPrintFormatId(reportInfo.getPrintFormatId())
		.setReportViewId(reportInfo.getReportViewId())
		.setRecordCount(reportInfo.getRecordCount())
		.addAllColumns(
				reportInfo.getColumns().stream().map(
						column -> ReportColumn.newBuilder()
						.setCode(ValueManager.validateNull(column.getCode()))
						.setTitle(ValueManager.validateNull(column.getTitle()))
						.setColor(ValueManager.validateNull(column.getColor()))
						.setStyle(ValueManager.validateNull(column.getStyle()))
						.build()
						).collect(Collectors.toList())
				)
		;
		List<ReportRow> reportRows = new ArrayList<ReportRow>();
		reportInfo.getRowsAsTree().forEach(row -> reportRows.add(processParent(reportInfo.getColumns(), row).build()));
		builder.addAllRows(reportRows);
		builder.setRecordCount(reportInfo.getRecordCount());
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken((int) reportInfo.getRecordCount(), offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);
		return builder;
	}
	
	private static ReportRow.Builder convertRow(List<ColumnInfo> columns, Row row) {
		Struct.Builder cells = Struct.newBuilder();
		columns.forEach(column -> {
			Struct.Builder cellValue = Struct.newBuilder();
			Cell cell = row.getCell(column.getPrintFormatItemId());
			//	Put Value
			cellValue.putFields(VALUE_KEY, ValueManager.getValueFromObject(cell.getValue()).build());
			//	Put Display Value
			cellValue.putFields(DISPLAY_VALUE_KEY, ValueManager.getValueFromString(cell.getDisplayValue()).build());
			//	Summary Values
			if(cell.getSum() != null) {
				cellValue.putFields(SUM_KEY, ValueManager.getValueFromObject(cell.getSum()).build());
			}
			if(cell.getMean() != null) {
				cellValue.putFields(MEAN_KEY, ValueManager.getValueFromObject(cell.getMean()).build());
			}
			if(cell.getCount() != null) {
				cellValue.putFields(COUNT_KEY, ValueManager.getValueFromObject(cell.getCount()).build());
			}
			if(cell.getMinimum() != null) {
				cellValue.putFields(MINIMUM_KEY, ValueManager.getValueFromObject(cell.getMinimum()).build());
			}
			if(cell.getMaximum() != null) {
				cellValue.putFields(MAXIMUM_KEY, ValueManager.getValueFromObject(cell.getMaximum()).build());
			}
			if(cell.getVariance() != null) {
				cellValue.putFields(VARIANCE_KEY, ValueManager.getValueFromObject(cell.getVariance()).build());
			}
			if(cell.getDeviation() != null) {
				cellValue.putFields(DEVIATION_KEY, ValueManager.getValueFromObject(cell.getDeviation()).build());
			}
			
			//	Put Cell Value
			cells.putFields("" + column.getPrintFormatItemId(), Value.newBuilder().setStructValue(cellValue.build()).build());
		});
		return ReportRow.newBuilder().setCells(cells).setLevel(row.getLevel());
	}
	
	private static ReportRow.Builder processParent(List<ColumnInfo> columns, Row row) {
		ReportRow.Builder parentRow = convertRow(columns, row);
		row.getChildren().forEach(child -> processChildren(columns, parentRow, child));
		return parentRow;
	}
	
	private static void processChildren(List<ColumnInfo> columns, ReportRow.Builder parentBuilderRow, Row parent) {
		ReportRow.Builder childValue = convertRow(columns, parent);
		List<Row> children = parent.getChildren();
		if(children.size() > 0) {
			children.forEach(child -> processChildren(columns, childValue, child));
		}
		parentBuilderRow.addChildren(childValue);
	}
}
