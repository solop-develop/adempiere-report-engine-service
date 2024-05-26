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
import org.spin.report_engine.data.ReportInfo;
import org.spin.report_engine.data.Row;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.service.grpc.util.value.ValueManager;

public class Service {
		
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
		ReportInfo reportInfo = ReportBuilder.newInstance(request.getId())
				.withFilters(FilterManager.newInstance(request.getFilters())
				.getConditions())
				.run(100, 0);
		//	
		Report.Builder builder = Report.newBuilder();
		builder.setName(ValueManager.validateNull(reportInfo.getName()))
		.setDescription(ValueManager.validateNull(reportInfo.getDescription()))
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
		reportInfo.getRowsAsTree().forEach(row -> reportRows.add(convertRow(row).build()));
		return builder;
	}
	
	private static ReportRow.Builder convertRow(Row row) {
		ReportRow.Builder builder = ReportRow.newBuilder();
		return builder;
	}
}
