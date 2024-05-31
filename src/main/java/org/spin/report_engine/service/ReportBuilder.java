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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_AD_PrintFormat;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoUtil;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.spin.report_engine.data.Cell;
import org.spin.report_engine.data.ReportInfo;
import org.spin.report_engine.format.PrintFormat;
import org.spin.report_engine.format.QueryDefinition;
import org.spin.service.grpc.util.db.LimitUtil;
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
	private int recordId;
	private int limit;
	private int offset;
	private int instanceId;
	
	/**	Multi-Selection Parameters	*/
    private LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection;
	private String aliasTableSelection;
	private int tableSelectionId;
	private List<Integer> selectedRecordsIds;
	
	
	private ReportBuilder() {
		conditions = new ArrayList<Filter>();
		selectedRecordsIds = new ArrayList<>();
		selection = new LinkedHashMap<Integer, LinkedHashMap<String,Object>>();
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
		conditions.add(filter);
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
		this.instanceId = instanceId;
		return this;
	}

	/**
	 * Static constructor
	 * @param printFormatId
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
		return this;
	}

	public int getReportId() {
		return reportId;
	}

	public ReportBuilder withReportId(int reportId) {
		this.reportId = reportId;
		return this;
	}

	public int getReportViewId() {
		return reportViewId;
	}

	public ReportBuilder withReportViewId(int reportViewId) {
		this.reportViewId = reportViewId;
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
		limit = LimitUtil.getPageSize(limit);
		MPrintFormat printFormat = new MPrintFormat(Env.getCtx(), getPrintFormatId(), null);
		PrintFormat format = PrintFormat.newInstance(printFormat);
		QueryDefinition queryDefinition = format.getQuery().withConditions(conditions).withLimit(limit, offset).buildQuery();
		ReportInfo reportInfo = ReportInfo.newInstance(format, queryDefinition);
		DB.runResultSet(transactionName, queryDefinition.getCompleteQuery(), queryDefinition.getParameters(), resulset -> {
			while (resulset.next()) {
				format.getItems().forEach(item -> {
					Map<String, Cell> cells = new HashMap<String, Cell>();
					queryDefinition.getColumns()
					.stream()
					.filter(column -> column.getColumnName().equals(item.getColumnName()))
					.forEach(column -> {
						Cell cell = Optional.ofNullable(cells.get(column.getColumnName())).orElse(Cell.newInstance());
						try {
							if(column.isDisplayValue()) {
								cell.withDisplayValue(resulset.getString(column.getColumnNameAlias()));
							} else {
								if(DisplayType.isLookup(column.getReferenceId()) && column.getReferenceId() != DisplayType.List) {
									cell.withValue(resulset.getInt(column.getColumnName()));
								} else {
									cell.withValue(resulset.getObject(column.getColumnName()));
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						cells.put(item.getColumnName(), cell);
					});
					reportInfo.addCell(item, cells.get(item.getColumnName()));
				});
				reportInfo.addRow();
			}
		});
		return reportInfo.completeInfo();
	}
	
	public ReportInfo run() {
		if (reportId == 0 && printFormatId == 0) {
			throw new AdempiereException("@AD_Process_ID@ @NotFound@");
		}
		AtomicReference<ReportInfo> reportInfo = new AtomicReference<ReportInfo>();
		//	Run Process before get
		Trx.run(transactionName -> {
			if(reportId > 0) {
				ReportProcessor.newInstance().withProcessInfo(generateProcessInfo(transactionName)).withTransactionName(transactionName).run();
			}
			validatePrintFormat(transactionName);
			reportInfo.set(get(transactionName));
		});
		return reportInfo.get();
	}
	
	private void validatePrintFormat(String transactionName) {
		MProcess report = MProcess.get(Env.getCtx(), reportId);
		if(printFormatId == 0 && report.getAD_ReportView_ID() > 0) {
			printFormatId = new Query(Env.getCtx(), I_AD_PrintFormat.Table_Name, I_AD_PrintFormat.COLUMNNAME_AD_ReportView_ID + " = ?", transactionName)
			.setParameters(report.getAD_ReportView_ID())
			.setOrderBy(I_AD_PrintFormat.COLUMNNAME_AD_Client_ID + " DESC, " + I_AD_PrintFormat.COLUMNNAME_IsDefault + " DESC")
			.firstId();
			//	Create It
			if(printFormatId == 0) {
				MClient client = MClient.get(Env.getCtx());
				MPrintFormat printFormat = MPrintFormat.createFromReportView(Env.getCtx(), report.getAD_ReportView_ID(), client.getName() + ": " + report.get_Translation(I_AD_Process.COLUMNNAME_Name));
				printFormatId = printFormat.getAD_PrintFormat_ID();
			}
		}
	}
	
	private MPInstance generateProcessInstance() {
		//	Generate Process here
		MProcess process = MProcess.get(Env.getCtx(), reportId);
		MPInstance processInstance = new MPInstance(Env.getCtx(), reportId, recordId);
		processInstance.setAD_Process_ID(process.getAD_Process_ID());
		processInstance.setName(process.get_Translation(I_AD_Process.COLUMNNAME_Name));
		processInstance.setRecord_ID(recordId);
		processInstance.saveEx();
		withInstanceId(processInstance.getAD_PInstance_ID());
		//	Add Parameters
		AtomicInteger sequence = new AtomicInteger(0);
		conditions.forEach(filter -> {
			MProcessPara processParameter = process.getParameter(filter.getColumnName());
			if(processParameter != null) {
				Object value = ParameterUtil.getValueToFilterRestriction(processParameter.getAD_Reference_ID(), filter.getValue());
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
		 boolean isSelection = selectedRecordsIds.size() > 0;
		 processInfo = new ProcessInfo(process.get_Translation(I_AD_Process.COLUMNNAME_Name), reportId, tableId, recordId, false);
		 processInfo.setAD_PInstance_ID(processInstance.getAD_PInstance_ID());
		 processInfo.setClassName(process.getClassname());
		 processInfo.setTransactionName(transactionName);
		 processInfo.setIsSelection(isSelection);
		 processInfo.setPrintPreview(false);
		 processInfo.setAliasForTableSelection(aliasTableSelection);
		 processInfo.setAD_Client_ID(processInstance.getAD_Client_ID());
		 processInfo.setAD_User_ID(processInstance.getAD_User_ID());
		 processInfo.setReportType(processInstance.getReportType());
		 processInfo.setIsBatch(false);
		 ProcessInfoUtil.setParameterFromDB(processInfo);
		 //	FR [ 352 ]
		 if (isSelection) {
			 processInfo.setSelectionKeys(selectedRecordsIds);
			 processInfo.setTableSelectionId(tableSelectionId);
			 processInfo.setSelectionValues(selection);
		 }
		 
		 return processInfo;
	}
	
	/**
     * Define table and Record id for this process
     * @param tableId
     * @param recordId
     * @return
     */
    public ReportBuilder withRecordId(int tableId , int recordId) {
        this.tableId =  tableId;
        this.recordId = recordId;
        return this;
    }
	
    /**
     * Define mutiples select record ids to be processed
     * @param selectedRecordsIds
     * @return
     */
    public ReportBuilder withSelectedRecordsIds(int tableSelectionId, List<Integer> selectedRecordsIds) {
        return withSelectedRecordsIds(tableSelectionId, null, selectedRecordsIds);
    }
    
    /**
     * Define mutiples select record ids to be processed
     * @param tableSelectionId
     * @param aliasTableSelection
     * @param selectedRecordsIds
     * @return
     */
    public ReportBuilder withSelectedRecordsIds(int tableSelectionId, String aliasTableSelection, List<Integer> selectedRecordsIds) {
        this.selectedRecordsIds = selectedRecordsIds;
        this.tableSelectionId = tableSelectionId;
        this.aliasTableSelection = aliasTableSelection;
        return this;
    }

    /**
     * Define select record ids and values
     * @param selectedRecordsIds
     * @param selection
     * @return
     */
    public ReportBuilder withSelectedRecordsIds(int tableSelectionId , List<Integer> selectedRecordsIds, LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection)
    {
        this.selectedRecordsIds = selectedRecordsIds;
        this.selection = selection;
        this.tableSelectionId = tableSelectionId;
        return this;
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
			.get(null);
	}
}
