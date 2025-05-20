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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Menu;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRecentItem;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.report_engine.ReportColumn;
import org.spin.backend.grpc.report_engine.ReportRow;
import org.spin.backend.grpc.report_engine.RunExportRequest;
import org.spin.backend.grpc.report_engine.RunExportResponse;
import org.spin.backend.grpc.report_engine.SystemInfo;
import org.spin.base.Version;
import org.spin.backend.grpc.report_engine.GetReportRequest;
import org.spin.backend.grpc.report_engine.Report;
import org.spin.report_engine.data.Cell;
import org.spin.report_engine.data.ColumnInfo;
import org.spin.report_engine.data.ReportInfo;
import org.spin.report_engine.data.Row;
import org.spin.report_engine.export.XlsxExporter;
import org.spin.report_engine.format.QueryDefinition;
import org.spin.report_engine.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

public class Service {
	private static final String VALUE_KEY = "value";
	private static final String DISPLAY_VALUE_KEY = "display_value";
	private static final String TABLE_NAME_KEY = "table_name";
	private static final String SUM_KEY = "sum_value";
	private static final String MEAN_KEY = "mean_value";
	private static final String COUNT_KEY = "count_value";
	private static final String MINIMUM_KEY = "minimum_value";
	private static final String MAXIMUM_KEY = "maximum_value";
	private static final String VARIANCE_KEY = "variance_value";
	private static final String DEVIATION_KEY = "deviation_value";


	/** Supported file extensions to enable export */
	public static final List<String> SUPPORTED_EXPORT_EXTENSIONS = Arrays.asList(
		"xlsx"
	);



	/**
	 * Add element to recent item
	 * @param reportId
	 */
	public static void addToRecentItem(int reportId) {
		if (reportId <= 0) {
			return;
		}
		final String whereClause = I_AD_Process.COLUMNNAME_AD_Process_ID + " = ?";
		//	Get menu
		int menuId = new Query(
			Env.getCtx(),
			I_AD_Menu.Table_Name,
			whereClause,
			null
		)
			.setParameters(reportId)
			.firstId()
		;
		if (menuId <= 0) {
			return;
		}
		MRecentItem.addMenuOption(Env.getCtx(), menuId, 0);
	}


	public static SystemInfo.Builder getSystemInfo() {
		SystemInfo.Builder builder = SystemInfo.newBuilder();

		// backend info
		builder.setDateVersion(
				ValueManager.getTimestampFromDate(
					TimeManager.getTimestampFromString(
						Version.DATE_VERSION
					)
				)
			)
			.setMainVersion(
				StringManager.getValidString(
					Version.MAIN_VERSION
				)
			)
			.setImplementationVersion(
				StringManager.getValidString(
					Version.IMPLEMENTATION_VERSION
				)
			)
		;

		return builder;
	}


	/**
	 * Get a View after run report
	 * @param context
	 * @param request
	 * @return
	 */
	public static Report.Builder getView(GetReportRequest request) {
		if(request.getPrintFormatId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_PrintFormat_ID@");
		}
		//	Add to recent Item
		addToRecentItem(
			request.getReportId()
		);

		ReportBuilder reportBuilder = ReportBuilder.newInstance()
			.withPrintFormatId(request.getPrintFormatId())
			.withReportViewId(request.getReportViewId())
			.withSummary(request.getIsSummary())
			.withInstanceId(request.getInstanceId())
		;

		// Parameters as filters
		if(!Util.isEmpty(request.getFilters(), true)) {
			List<Filter> conditionsList = FilterManager.newInstance(request.getFilters())
				.getConditions()
			;
			reportBuilder.withFilters(conditionsList);
		}

		// Window table name and record identifier
		if (!Util.isEmpty(request.getTableName(), true)) {
			MTable table = RecordUtil.validateAndGetTable(
				request.getTableName()
			);
			RecordUtil.validateRecordId(
				request.getRecordId(),
				table.getAccessLevel()
			);
			reportBuilder.withRecordId(
				table.getAD_Table_ID(),
				request.getRecordId()
			);
		}

		// Fill pagination
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		ReportInfo reportInfo = reportBuilder.withLimit(limit).withOffset(offset).run();
		return convertReport(reportInfo, limit, offset, pageNumber, request.getShowAsRows());
	}


	/**
	 * Run Report
	 * @param context
	 * @param request
	 * @return
	 */
	public static Report.Builder getReport(GetReportRequest request) {
		if(request.getReportId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
		}
		//	Add to recent Item
		addToRecentItem(
			request.getReportId()
		);

		ReportBuilder reportBuilder = ReportBuilder.newInstance()
			.withReportId(request.getReportId())
			.withPrintFormatId(request.getPrintFormatId())
			.withReportViewId(request.getReportViewId())
			.withSummary(request.getIsSummary())
		;

		// Parameters as filters
		if(!Util.isEmpty(request.getFilters(), true)) {
			List<Filter> conditionsList = FilterManager.newInstance(request.getFilters())
				.getConditions()
			;
			reportBuilder.withFilters(conditionsList);
		}

		// Window table name and record identifier
		if (!Util.isEmpty(request.getTableName(), true)) {
			MTable table = RecordUtil.validateAndGetTable(
				request.getTableName()
			);
			RecordUtil.validateRecordId(
				request.getRecordId(),
				table.getAccessLevel()
			);
			reportBuilder.withRecordId(
				table.getAD_Table_ID(),
				request.getRecordId()
			);
		}

		// Fill pagination
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		ReportInfo reportInfo = reportBuilder.withLimit(limit).withOffset(offset).run();
		return convertReport(reportInfo, limit, offset, pageNumber, request.getShowAsRows());
	}


	/**
	 * Run Export Report
	 * @param context
	 * @param request
	 * @return
	 */
	public static RunExportResponse.Builder getExportReport(RunExportRequest request) {
		if(request.getReportId() <= 0 && request.getPrintFormatId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@ / @AD_PrintFormat_ID@");
		}
		//	Add to recent Item
		addToRecentItem(
			request.getReportId()
		);

		// Validate extension to export file
		String format = request.getFormat();
		if (Util.isEmpty(format, true)) {
			format = "xlsx";
		}
		if (!SUPPORTED_EXPORT_EXTENSIONS.contains(format)) {
			throw new AdempiereException(
				"@FileInvalidExtension@. @EXP_Format_ID@: " + SUPPORTED_EXPORT_EXTENSIONS.toString()
			);
		}

		ReportBuilder reportBuilder = ReportBuilder.newInstance()
			.withReportId(request.getReportId())
			.withPrintFormatId(request.getPrintFormatId())
			.withReportViewId(request.getReportViewId())
			.withSummary(request.getIsSummary())
		;

		// Parameters as filters
		if(!Util.isEmpty(request.getFilters(), true)) {
			List<Filter> conditionsList = FilterManager.newInstance(request.getFilters())
				.getConditions()
			;
			reportBuilder.withFilters(conditionsList);
		}

		// Window table name and record identifier
		if (!Util.isEmpty(request.getTableName(), true)) {
			MTable table = RecordUtil.validateAndGetTable(
				request.getTableName()
			);
			RecordUtil.validateRecordId(
				request.getRecordId(),
				table.getAccessLevel()
			);
			reportBuilder.withRecordId(
				table.getAD_Table_ID(),
				request.getRecordId()
			);
		}

		// Fill pagination
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = QueryDefinition.NO_LIMIT;
		int offset = 0;
		if(request.getPageSize() > 0) {
			limit = request.getPageSize();
			offset = (pageNumber - 1) * limit;
		}

		ReportInfo reportInfo = reportBuilder.withLimit(limit).withOffset(offset).run();
		RunExportResponse.Builder builder = RunExportResponse.newBuilder()
			.setInstanceId(
				reportInfo.getInstanceId()
			)
		;

		String fileName = XlsxExporter.newInstance().export(reportInfo);
		if (!Util.isEmpty(fileName, true)) {
			builder
				.setFileName(
					StringManager.getValidString(fileName)
				)
			;
		}
		return builder;
	}


	private static Report.Builder convertReport(ReportInfo reportInfo, int limit, int offset, int pageNumber, boolean showAsRow) {
		//	
		Report.Builder builder = Report.newBuilder()
			.setName(
				StringManager.getValidString(
					reportInfo.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					reportInfo.getDescription()
				)
			)
			.setId(
				reportInfo.getPrintFormatId()
			)
			.setPrintFormatId(
				reportInfo.getPrintFormatId()
			)
			.setReportViewId(
				reportInfo.getReportViewId()
			)
			.setRecordCount(
				reportInfo.getRecordCount()
			)
			.setInstanceId(
				reportInfo.getInstanceId()
			)
			.setTableName(
				StringManager.getValidString(
					reportInfo.getTableName()
				)
			)
		;

		List<ReportColumn> reportColumnsList = reportInfo.getColumns()
			.stream()
			.map(column -> {
				ReportColumn.Builder columnBuilder = convertColumn(column);
				return columnBuilder.build();
			})
			.collect(Collectors.toList())
		;
		if (reportColumnsList != null && reportColumnsList.size() > 0) {
			builder.addAllColumns(reportColumnsList);
		}

		List<ReportRow> reportRows = new ArrayList<ReportRow>();
		if(showAsRow) {
			reportInfo.getCompleteRows().forEach(row -> {
				ReportRow.Builder rowBuilder = convertRow(
					reportInfo.getColumns(),
					row
				);
				reportRows.add(
					rowBuilder.build()
				);
			});
		} else {
			reportInfo.getRowsAsTree().forEach(row -> {
				ReportRow.Builder rowBuilder = processParent(
					reportInfo.getColumns(),
					row
				);
				reportRows.add(
					rowBuilder.build()
				);
			});
		}
		builder.addAllRows(reportRows);
		builder.setRecordCount(reportInfo.getRecordCount());
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken((int) reportInfo.getRecordCount(), offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix("") + String.valueOf(pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		builder.setReportViewId(reportInfo.getReportViewId());
		return builder;
	}
	
	private static ReportColumn.Builder convertColumn(ColumnInfo column) {
		ReportColumn.Builder columnBuilder = ReportColumn.newBuilder()
			.setCode(
				StringManager.getValidString(
					column.getCode()
				)
			)
			.setTitle(
				StringManager.getValidString(
					column.getTitle()
				)
			)
			.setColor(
				StringManager.getValidString(
					column.getColor()
				)
			)
			.setStyle(
				StringManager.getValidString(
					column.getStyle()
				)
			)
			.setFontCode(
				StringManager.getValidString(
					column.getFontCode()
				)
			)
			.setColumnName(
				StringManager.getValidString(
					column.getColumnName()
				)
			)
			.setIsFixedWidth(
				column.isFixedWidth()
			)
			.setColumnWidth(
				column.getColumnWidth()
			)
			.setColumnCharactersSize(
				column.getColumnCharactersSize()
			)
			.setDisplayType(
				column.getDisplayTypeId()
			)
			.setIsGroupColumn(
				column.isGroupColumn()
			)
			.setSequence(
				column.getSequence()
			)
			.setIsHideGrandTotal(
				column.isHideGrandTotal()
			)
		;
		return columnBuilder;
	}

	private static ReportRow.Builder convertRow(List<ColumnInfo> columns, Row row) {
		Struct.Builder cells = Struct.newBuilder();
		columns.forEach(column -> {
			Struct.Builder cellValue = Struct.newBuilder();
			Cell cell = row.getCell(column.getPrintFormatItemId());
			//	Put Value
			cellValue.putFields(VALUE_KEY, ValueManager.getValueFromObject(cell.getValue()).build());
			//	Table Name Reference
			if(!Util.isEmpty(cell.getTableName())) {
				cellValue.putFields(TABLE_NAME_KEY, ValueManager.getValueFromObject(cell.getTableName()).build());
			}
			//	Put Display Value
			cellValue.putFields(DISPLAY_VALUE_KEY, ValueManager.getValueFromString(cell.getDisplayValue()).build());
			//	Summary Values
			if(cell.getSum() != null) {
				cellValue.putFields(SUM_KEY, convertFunctionDisplayValue(ValueManager.getValueFromObject(cell.getSum()), cell.getSumDisplayValue()));
			}
			if(cell.getMean() != null) {
				cellValue.putFields(MEAN_KEY, convertFunctionDisplayValue(ValueManager.getValueFromObject(cell.getMean()), cell.getMeanDisplayValue()));
			}
			if(cell.getCount() != null) {
				cellValue.putFields(COUNT_KEY, convertFunctionDisplayValue(ValueManager.getValueFromObject(cell.getCount()), cell.getCountDisplayValue()));
			}
			if(cell.getMinimum() != null) {
				cellValue.putFields(MINIMUM_KEY, convertFunctionDisplayValue(ValueManager.getValueFromObject(cell.getMinimum()), cell.getMinimumDisplayValue()));
			}
			if(cell.getMaximum() != null) {
				cellValue.putFields(MAXIMUM_KEY, convertFunctionDisplayValue(ValueManager.getValueFromObject(cell.getMaximum()), cell.getMaximumDisplayValue()));
			}
			if(cell.getVariance() != null) {
				cellValue.putFields(VARIANCE_KEY, convertFunctionDisplayValue(ValueManager.getValueFromObject(cell.getVariance()), cell.getVarianceDisplayValue()));
			}
			if(cell.getDeviation() != null) {
				cellValue.putFields(DEVIATION_KEY, convertFunctionDisplayValue(ValueManager.getValueFromObject(cell.getDeviation()), cell.getDeviationDisplayValue()));
			}
			//	Put Cell Value
			cells.putFields("" + column.getPrintFormatItemId(), Value.newBuilder().setStructValue(cellValue.build()).build());
		});
		boolean isParent = Optional.ofNullable(row.getChildren()).orElse(new ArrayList<>()).size() > 0;
		return ReportRow.newBuilder().setCells(cells).setLevel(row.getLevel()).setIsParent(isParent);
	}
	
	private static Value convertFunctionDisplayValue(Value.Builder currentValue, String displayValue) {
		Struct.Builder struct = currentValue.getStructValueBuilder();
		struct.putFields(DISPLAY_VALUE_KEY, ValueManager.getValueFromString(displayValue).build());
		return Value.newBuilder().setStructValue(struct).build();
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
