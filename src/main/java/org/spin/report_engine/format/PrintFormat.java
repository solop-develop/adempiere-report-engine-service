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
package org.spin.report_engine.format;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_PrintFormatItem;
import org.adempiere.core.domains.models.I_AD_ReportView;
import org.compiere.model.MReportView;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.print.MPrintFormatItem;
import org.compiere.util.Env;

/**
 * Print Format Representation
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class PrintFormat {
	
	private String name;
	private String description;
	private int printFormatId;
	private ReportView reportView;
	private int tableId;
	private boolean isSummary;
	private List<ReportView> reportViews;
	private List<PrintFormatItem> items;
	
	private PrintFormat(MPrintFormat printFormat) {
		name = printFormat.getName();
		description = printFormat.getDescription();
		printFormatId = printFormat.getAD_PrintFormat_ID();
		if(printFormat.getAD_ReportView_ID() > 0) {
			reportView = ReportView.newInstance(new MReportView(Env.getCtx(), printFormat.getAD_ReportView_ID(), null));
		}
		tableId = printFormat.getAD_Table_ID();
		isSummary = printFormat.isSummary();
		//	Get Views
		reportViews = new ArrayList<ReportView>();
		new Query(Env.getCtx(), I_AD_ReportView.Table_Name, I_AD_ReportView.COLUMNNAME_AD_Table_ID + " = ?", null)
		.setParameters(tableId)
		.getIDsAsList()
		.forEach(reportViewId -> reportViews.add(ReportView.newInstance(new MReportView(Env.getCtx(), reportViewId, null))));
		//	Get Items
		items = new ArrayList<PrintFormatItem>();
		new Query(Env.getCtx(), I_AD_PrintFormatItem.Table_Name, I_AD_PrintFormatItem.COLUMNNAME_AD_PrintFormat_ID + " = ?", null)
		.setParameters(printFormatId)
		.getIDsAsList()
		.forEach(printFormatItemId -> items.add(PrintFormatItem.newInstance(new MPrintFormatItem(Env.getCtx(), printFormatItemId, null))));
	}
	
	public static PrintFormat newInstance(MPrintFormat printFormat) {
		return new PrintFormat(printFormat);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int getPrintFormatId() {
		return printFormatId;
	}

	public ReportView getReportView() {
		return reportView;
	}

	public int getTableId() {
		return tableId;
	}

	public boolean isSummary() {
		return isSummary;
	}

	public List<ReportView> getReportViews() {
		return reportViews;
	}

	public List<PrintFormatItem> getItems() {
		return items;
	}

	@Override
	public String toString() {
		return "PrintFormat [name=" + name + ", description=" + description + ", printFormatId=" + printFormatId
				+ ", reportView=" + reportView + ", tableId=" + tableId + ", isSummary=" + isSummary + ", items="
				+ items + "]";
	}
}
