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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.print.MPrintFormat;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.spin.report_engine.data.ReportInfo;
import org.spin.report_engine.format.PrintFormat;
import org.spin.service.grpc.util.query.Filter;

/**
 * A builder that allows load a report definition from data and run
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 *
 */
public class ReportBuilder {
	
	private int printFormatId;
	private int reportViewId;
	private boolean isSummary;
	private List<Filter> conditions;
	
	
	private ReportBuilder(int printFormatId) {
		this.printFormatId = printFormatId;
		conditions = new ArrayList<Filter>();
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
	
	/**
	 * Static constructor
	 * @param printFormatId
	 * @return
	 */
	public static ReportBuilder newInstance(int printFormatId) {
		return new ReportBuilder(printFormatId);
	}

	public int getPrintFormatId() {
		return printFormatId;
	}

	public ReportBuilder withPrintFormatId(int printFormatId) {
		this.printFormatId = printFormatId;
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
	
	public ReportInfo run(int pageSize, String nextPageToken) {
		if(getPrintFormatId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_PrintFormat_ID@");
		}
		MPrintFormat printFormat = new MPrintFormat(Env.getCtx(), getPrintFormatId(), null);
		PrintFormat format = PrintFormat.newInstance(printFormat);
		
		DB.runResultSet(null, "", null, resulset -> {
			
		});
		System.out.println(format.getQuery());
		return ReportInfo.newInstance();
	}
	
	public static void main(String[] args) {
		//	50132
		//	Stocktake Line
		org.compiere.Adempiere.startup(true);
		Env.setContext(Env.getCtx(), "#AD_Client_ID", 11);
		Env.setContext(Env.getCtx(), Env.LANGUAGE, "es_MX");
		Env.setContext(Env.getCtx(), "#AD_Role_ID", 102);
		ReportBuilder.newInstance(50132).run(50, null);
	}
}
