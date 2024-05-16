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

import org.adempiere.core.domains.models.I_AD_ReportView;
import org.compiere.model.MReportView;
import org.compiere.util.Util;

/**
 * Report View Representation
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ReportView {
	
	private int reportViewId;
	private String name;
	private String title;
	private String whereClause;
	
	private ReportView(MReportView reportView) {
		reportViewId = reportView.getAD_ReportView_ID();
		name = reportView.getName();
		title = reportView.get_Translation(I_AD_ReportView.COLUMNNAME_PrintName);
		if(Util.isEmpty(title)) {
			title = name;
		}
		whereClause = reportView.getWhereClause();
	}
	
	public static ReportView newInstance(MReportView reportView) {
		return new ReportView(reportView);
	}

	public int getReportViewId() {
		return reportViewId;
	}
	
	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public String getWhereClause() {
		return whereClause;
	}

	@Override
	public String toString() {
		return "ReportView [reportViewId=" + reportViewId + ", name=" + name + ", title=" + title + ", whereClause="
				+ whereClause + "]";
	}
}
