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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_AD_PrintFormat;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MColumn;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MReportView;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoUtil;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.report_engine.data.Cell;
import org.spin.report_engine.data.ReportInfo;
import org.spin.report_engine.format.PrintFormat;
import org.spin.report_engine.format.QueryDefinition;
import org.spin.report_engine.mapper.DefaultMapping;
import org.spin.report_engine.mapper.IColumnMapping;
import org.spin.report_engine.util.ClassLoaderMapping;
import org.spin.report_engine.util.RecordUtil;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.query.Filter;

/**
 * A builder that allows load a report definition from data and run
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 *
 */
public class ReportBuilder {
	
	private int printFormatId;
	private int reportId;
	private int reportViewId;
	private boolean isSummary;
	private List<Filter> conditions;
	private int tableId;
	private int recordId = -1; // Some records with zero are valid (`*` = all)
	private int limit;
	private int offset;
	private int instanceId;
	
	private static final CLogger logger = CLogger.getCLogger(ReportBuilder.class);
	
	private ReportBuilder() {
		conditions = new ArrayList<Filter>();
	}

	public List<Filter> getFilters() {
		return this.conditions;
	}

	public ReportBuilder withFilters(List<Filter> filters) {
		this.conditions = filters;
		return this;
	}


	public ReportBuilder withParameter(String key, Object value) {
		Map<String, Object> condition = new HashMap<>();
		condition.put(Filter.OPERATOR, Filter.EQUAL);
		condition.put(Filter.VALUES, value);
		Filter filter = new Filter(condition);
		filter.setColumnName(key);
		this.conditions.add(filter);
		return this;
	}


	public int getLimit() {
		return limit;
	}

	public ReportBuilder withLimit(int limit) {
		this.limit = limit;
		return this;
	}


	public int getOffset() {
		return offset;
	}

	public ReportBuilder withOffset(int offset) {
		this.offset = offset;
		return this;
	}


	public int getInstanceId() {
		return instanceId;
	}

	public ReportBuilder withInstanceId(int instanceId) {
		if(instanceId > 0) {
			this.instanceId = instanceId;
			withParameter("AD_PInstance_ID", instanceId);
		}
		return this;
	}


	/**
	 * Static constructor
	 * @return
	 */
	public static ReportBuilder newInstance() {
		return new ReportBuilder();
	}


	public int getPrintFormatId() {
		return printFormatId;
	}

	public ReportBuilder withPrintFormatId(int printFormatId) {
		this.printFormatId = printFormatId;
		if (printFormatId > 0) {
			MPrintFormat printFormat = new MPrintFormat(Env.getCtx(), printFormatId, null);
			if (printFormat == null || printFormat.getAD_PrintFormat_ID() <= 0) {
				throw new AdempiereException("@AD_PrintFormat_ID@ (" + printFormatId + ") @NotFound@");
			}
		}
		return this;
	}


	public int getReportId() {
		return reportId;
	}

	public ReportBuilder withReportId(int reportId) {
		this.reportId = reportId;
		if (reportId > 0) {
			MProcess reportDefinition = MProcess.get(Env.getCtx(), reportId);
			if (reportDefinition == null || reportDefinition.getAD_Process_ID() <= 0) {
				throw new AdempiereException("@IsReport@ @AD_Process_ID@ (" + reportId + ") @NotFound@");
			}
		}
		return this;
	}


	public int getReportViewId() {
		return reportViewId;
	}

	public ReportBuilder withReportViewId(int reportViewId) {
		this.reportViewId = reportViewId;
		if (reportViewId > 0) {
			MReportView reportView = MReportView.get(Env.getCtx(), reportViewId);
			if (reportView == null || reportView.getAD_ReportView_ID() <= 0) {
				throw new AdempiereException("@AD_ReportView_ID@ (" + reportViewId + ") @NotFound@");
			}
		}
		return this;
	}


	public boolean isSummary() {
		return isSummary;
	}

	public ReportBuilder withSummary(boolean isSummary) {
		this.isSummary = isSummary;
		return this;
	}


	private ReportInfo get(String transactionName) {
		if(getPrintFormatId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_PrintFormat_ID@");
		}
		Language language = Language.getLoginLanguage();
		MPrintFormat printFormat = new MPrintFormat(Env.getCtx(), getPrintFormatId(), null);
		PrintFormat format = PrintFormat.newInstance(printFormat);
		if (this.getReportViewId() > 0) {
			format.setReportViewId(this.getReportViewId());
		}
		QueryDefinition queryDefinition = format.getQuery()
			.withInstanceId(getInstanceId())
			.withTableName(format.getTableName())
			.withConditions(this.conditions)
			.withWhereClause(format.getReportViewWhereClause())
			.withLimit(limit, offset)
			.buildQuery()
		;
		//	Count
		int count = CountUtil.countRecords(queryDefinition.getCompleteQueryCount(), format.getTableName(), queryDefinition.getParameters(), transactionName);
		ReportInfo reportInfo = ReportInfo.newInstance(format, queryDefinition)
			.withReportViewId(getReportViewId())
			.withInstanceId(getInstanceId())
			.withRecordCount(count)
			.withSummary(isSummary())
		;
		DB.runResultSet(transactionName, queryDefinition.getCompleteQuery(), queryDefinition.getParameters(), resulset -> {
			while (resulset.next()) {
				format.getItems().forEach(item -> {
					Map<String, Cell> cells = new HashMap<String, Cell>();
					queryDefinition.getQueryColumns()
					.stream()
					.filter(column -> column.getColumnName().equals(item.getColumnName()))
					.forEach(column -> {
						Cell cell = Optional.ofNullable(cells.get(column.getColumnName())).orElse(Cell.newInstance());
						try {
							if(column.isDisplayValue()) {
								cell.withDisplayValue(resulset.getString(column.getColumnNameAlias()));
							} else {
								Object value = resulset.getObject(column.getColumnName());
								cell.withValue(value);
								//	Apply Default Mask
								if(!Util.isEmpty(item.getMappingClassName())) {
									IColumnMapping customMapping = ClassLoaderMapping.loadClass(item.getMappingClassName());
									if(customMapping != null) {
										customMapping.processValue(item, column, language, resulset, cell);
									}
								} else {
									DefaultMapping.newInstance().processValue(item, column, language, resulset, cell);
								}
							}
						} catch (Exception e) {
							logger.warning(e.getLocalizedMessage());
						}
						cells.put(item.getColumnName(), cell);
					});
					reportInfo.addCell(item, cells.get(item.getColumnName()));
				});
				if(format.getTableName().equals("T_Report")) {
					reportInfo.addRow(resulset.getInt("LevelNo"), resulset.getInt("SeqNo"));
				} else {
					reportInfo.addRow();
				}
			}
		}).onFailure(throwable -> {
			throw new AdempiereException(throwable);
		});
		return reportInfo.completeInfo();
	}


	public ReportInfo run() {
		if (getReportId() <= 0 && getPrintFormatId() <= 0) {
			throw new AdempiereException("@AD_Process_ID@ @NotFound@");
		}
		MProcess report = MProcess.get(Env.getCtx(), reportId);
		if(getReportViewId() <= 0) {
			withReportViewId(report.getAD_ReportView_ID());
		}
		if(getPrintFormatId() <= 0 && report.getAD_PrintFormat_ID() > 0) {
			withPrintFormatId(report.getAD_PrintFormat_ID());
		}
		AtomicReference<ReportInfo> reportInfo = new AtomicReference<ReportInfo>();
		//	Run Process before get
		Trx.run(transactionName -> {
			if(getReportId() > 0) {
				ReportProcessor.newInstance()
					.withProcessInfo(generateProcessInfo(transactionName))
					.withTransactionName(transactionName)
					.run()
				;
			}
			validatePrintFormat(transactionName);
			reportInfo.set(get(transactionName));
		});
		return reportInfo.get();
	}


	private void validatePrintFormat(String transactionName) {
		if(getPrintFormatId() <= 0 && getReportViewId() > 0) {
			final int somePrintFormatId = new Query(
				Env.getCtx(),
				I_AD_PrintFormat.Table_Name,
				I_AD_PrintFormat.COLUMNNAME_AD_ReportView_ID + " = ?",
				transactionName
			)
				.setParameters(getReportViewId())
				.setOrderBy(I_AD_PrintFormat.COLUMNNAME_AD_Client_ID + " DESC, " + I_AD_PrintFormat.COLUMNNAME_IsDefault + " DESC")
				.firstId()
			;
			withPrintFormatId(somePrintFormatId);
			//	Create It
			if(getPrintFormatId() <= 0) {
				MProcess report = MProcess.get(Env.getCtx(), getReportId());
				MClient client = MClient.get(Env.getCtx());
				MPrintFormat printFormat = MPrintFormat.createFromReportView(
					Env.getCtx(),
					getReportViewId(),
					client.getName() + ": " + report.get_Translation(I_AD_Process.COLUMNNAME_Name)
				);
				withPrintFormatId(printFormat.getAD_PrintFormat_ID());
			}
		}
	}


	private MPInstance generateProcessInstance() {
		//	Generate Process here
		MProcess process = MProcess.get(Env.getCtx(), getReportId());
		MPInstance processInstance = new MPInstance(Env.getCtx(), getReportId(), getRecordId());
		processInstance.setAD_Process_ID(process.getAD_Process_ID());
		processInstance.setName(process.get_Translation(I_AD_Process.COLUMNNAME_Name));
		processInstance.setRecord_ID(getRecordId());
		processInstance.saveEx();
		if (!Util.isEmpty(process.getClassname(), true)) {
			withInstanceId(processInstance.getAD_PInstance_ID());
		}
		//	Add Parameters
		AtomicInteger sequence = new AtomicInteger(0);
		conditions.forEach(filter -> {
			MProcessPara processParameter = process.getParameter(filter.getColumnName());
			if(processParameter != null) {
				Object value = ParameterUtil.getValueToFilterRestriction(processParameter.getAD_Reference_ID(), filter.getFromValue());
				processInstance.createParameter(sequence.addAndGet(10), filter.getColumnName(), value);
				if(filter.getToValue() != null) {
					Object valueTo = ParameterUtil.getValueToFilterRestriction(processParameter.getAD_Reference_ID(), filter.getToValue());
					processInstance.createParameter(sequence.addAndGet(10), filter.getColumnName() + "_To", valueTo);
				}
			}
		});
		return processInstance;
	}


	private ProcessInfo generateProcessInfo(String transactionName) {
		MPInstance processInstance = generateProcessInstance();
		ProcessInfo processInfo;
		MProcess process = MProcess.get(Env.getCtx(), reportId);
		processInfo = new ProcessInfo(process.get_Translation(I_AD_Process.COLUMNNAME_Name), reportId, tableId, recordId, false);
		processInfo.setAD_PInstance_ID(processInstance.getAD_PInstance_ID());
		processInfo.setClassName(process.getClassname());
		processInfo.setTransactionName(transactionName);
		processInfo.setIsSelection(false);
		processInfo.setPrintPreview(false);
		processInfo.setAD_Client_ID(processInstance.getAD_Client_ID());
		processInfo.setAD_User_ID(processInstance.getAD_User_ID());
		processInfo.setReportType(processInstance.getReportType());
		processInfo.setIsBatch(false);
		ProcessInfoUtil.setParameterFromDB(processInfo);
		return processInfo;
	}


	/**
	 * Define table and Record id for this process
	 * @param tableId
	 * @param recordId
	 * @return
	 */
	public ReportBuilder withRecordId(int tableId, int recordId) {
		this.tableId = tableId;
		if (tableId > 0) {
			MTable table = MTable.get(Env.getCtx(), tableId);
			if (table == null || table.getAD_Table_ID() <= 0) {
				throw new AdempiereException("@AD_Table_ID@ (" + tableId + ") @NotFound@");
			}
	
			RecordUtil.validateRecordId(
				recordId,
				table.getAccessLevel()
			);
			this.recordId = recordId;

			if(getPrintFormatId() <= 0) {
				MColumn column = table.getColumn("AD_PrintFormat_ID");
				if(column != null) {
					PO entity = table.getPO(recordId, null);
					if(entity != null && entity.get_ID() > 0) {
						withPrintFormatId(
							entity.get_ValueAsInt("AD_PrintFormat_ID")
						);
					}
				}
			}
		} else {
			logger.warning("Empty Table_ID, Record_I set to -1 from " + recordId);
			this.recordId = -1;
		}
		return this;
	}

	public int getRecordId() {
		return recordId;
	}


	public static void main(String[] args) {
		//	50132
		//	Stocktake Line
		org.compiere.Adempiere.startup(true);
		Env.setContext(Env.getCtx(), "#AD_Client_ID", 1000000);
		Env.setContext(Env.getCtx(), Env.LANGUAGE, "es_VE");
		Env.setContext(Env.getCtx(), "#AD_Role_ID", 102);
		ReportBuilder.newInstance().withPrintFormatId(1000971)
			.withParameter("IsSOTrx", true)
			.withParameter("DocStatus", "CO")
			.withParameter("SalesRep_ID", 1000263)
			.get(null)
		;
	}
}
