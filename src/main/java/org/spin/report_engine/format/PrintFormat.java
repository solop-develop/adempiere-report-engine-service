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
import java.util.Comparator;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_PrintFormatItem;
import org.adempiere.core.domains.models.I_AD_ReportView;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MReportView;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.print.MPrintFormatItem;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;

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
	private String tableName;
	private boolean isSummary;
	private List<ReportView> reportViews;
	private List<PrintFormatItem> items;
	private int aliasNumber;
	
	private PrintFormat(MPrintFormat printFormat) {
		name = printFormat.getName();
		description = printFormat.getDescription();
		printFormatId = printFormat.getAD_PrintFormat_ID();
		if(printFormat.getAD_ReportView_ID() > 0) {
			reportView = ReportView.newInstance(new MReportView(Env.getCtx(), printFormat.getAD_ReportView_ID(), null));
		}
		tableId = printFormat.getAD_Table_ID();
		tableName = MTable.getTableName(printFormat.getCtx(), printFormat.getAD_Table_ID());
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

	public String getTableName() {
		return tableName;
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
	
	public QueryDefinition getQuery() {
		clearTableAlias();
		StringBuffer query = new StringBuffer();
		StringBuffer orderBy = new StringBuffer();
		StringBuffer tableReferences = new StringBuffer();
		Language language = Language.getLoginLanguage();
		List<PrintFormatColumn> columns = new ArrayList<PrintFormatColumn>();
		getItems().stream()
		.filter(item -> item.isActive() && item.isPrinted())
		.sorted(Comparator.comparing(PrintFormatItem::getSequence))
		.forEach(item -> {
			if(item.getColumnId() > 0) {
				String columnName = null;
				String alias = null;
				if(query.length() > 0) {
					query.append(", ");
				}
				if(item.isVirtualColumn()) {
					columnName = "(" + item.getColumnSql() + ")";
					query.append(columnName);
					query.append(" AS ").append(item.getColumnName());
					alias = item.getColumnName();
					columns.add(PrintFormatColumn.newInstance(item).withColumnNameAlias(item.getColumnName()));
				} else {
					columnName = getQueryColumnName(item.getColumnName());
					query.append(columnName);
					alias = columnName;
					columns.add(PrintFormatColumn.newInstance(item).withColumnNameAlias(columnName));
				}
				//	Process Display Value
				if(item.getReferenceId() == DisplayType.TableDir
						|| (item.getReferenceId() == DisplayType.Search && item.getReferenceValueId() == 0)) {
					if(query.length() > 0) {
						query.append(", ");
					}
					if(item.isVirtualColumn()) {
						columnName = MLookupFactory.getLookup_TableDirEmbed(language, item.getColumnName(), getTableName(), "(" + item.getColumnSql() + ")");
					} else {
						columnName = MLookupFactory.getLookup_TableDirEmbed(language, item.getColumnName(), getTableName());
					}
					query.append("(").append(columnName).append(")");
					alias = getDisplayColumnName(item);
					query.append(" AS ").append(alias);
					columns.add(PrintFormatColumn.newInstance(item).withDisplayValue(true).withColumnNameAlias(getDisplayColumnName(item)));
				} else if(item.getReferenceId() == DisplayType.Table
						|| (item.getReferenceId() == DisplayType.Search && item.getReferenceValueId() != 0)) {
					addTableAlias();
					ColumnReference columnReference = ColumnReference.getColumnReference(item.getReferenceValueId());
					StringBuffer displayColumnValue = new StringBuffer();
					if(columnReference.isIsValueDisplayed()) {
						displayColumnValue.append(getQueryReferenceColumnName("Value"));
					}
					if(displayColumnValue.length() > 0) {
						displayColumnValue.append("|| '_' ||");
					}
					displayColumnValue.append(getQueryReferenceColumnName(columnReference.getDisplayColumn()));
					if(query.length() > 0) {
						query.append(", ");
					}
					columnName = displayColumnValue.toString();
					query.append("(").append(displayColumnValue).append(")");
					alias = getDisplayColumnName(item);
					query.append(" AS ").append(alias);
					columns.add(PrintFormatColumn.newInstance(item).withDisplayValue(true).withColumnNameAlias(getDisplayColumnName(item)));
					//	Add JOIN
					if(item.isMandatory()) {
						tableReferences.append(" INNER JOIN ");
					} else {
						tableReferences.append(" LEFT OUTER JOIN ");
					}
					tableReferences.append(columnReference.getTableName()).append(" ").append(getTableAlias()).append(" ON (")
					.append(getQueryReferenceColumnName(columnReference.getKeyColumn())).append("=").append(getQueryColumnName(item.getColumnName())).append(")");
					if(columnReference.isIsTranslated()) {
						tableReferences.append(" LEFT OUTER JOIN ")
						.append(columnReference.getTableName()).append("_Trl").append(" ").append(getTableAliasTrl()).append(" ON (")
						.append(getQueryReferenceColumnNameTrl(columnReference.getKeyColumn())).append(" = ").append(getQueryColumnName(item.getColumnName()))
						//	Reference ID
						.append(getLanguageCriteria(language)).append(")");
					}
				} else if(item.getReferenceId() == DisplayType.List 
						|| (item.getReferenceId() == DisplayType.Button && item.getReferenceValueId() != 0)) {
					addTableAlias();
					ColumnReference columnReference = ColumnReference.getColumnReferenceList(language);
					StringBuffer displayColumnValue = new StringBuffer();
					if(columnReference.isIsValueDisplayed()) {
						displayColumnValue.append(getQueryReferenceColumnName("Value"));
					}
					if(displayColumnValue.length() > 0) {
						displayColumnValue.append("|| '_' ||");
					}
					if(columnReference.isIsTranslated()) {
						displayColumnValue.append("COALESCE(").append(getQueryReferenceColumnNameTrl(columnReference.getDisplayColumn())).append(", ").append(getQueryReferenceColumnName(columnReference.getDisplayColumn())).append(")");
					} else {
						displayColumnValue.append(getQueryReferenceColumnName(columnReference.getDisplayColumn()));
					}
					if(query.length() > 0) {
						query.append(", ");
					}
					columnName = displayColumnValue.toString();
					query.append("(").append(displayColumnValue).append(")");
					alias = getDisplayColumnName(item);
					query.append(" AS ").append(alias);
					columns.add(PrintFormatColumn.newInstance(item).withDisplayValue(true).withColumnNameAlias(getDisplayColumnName(item)));
					//	Add JOIN
					if(item.isMandatory()) {
						tableReferences.append(" INNER JOIN ");
					} else {
						tableReferences.append(" LEFT OUTER JOIN ");
					}
					tableReferences.append(columnReference.getTableName()).append(" ").append(getTableAlias()).append(" ON (")
					.append(getQueryColumnName(item.getColumnName())).append(" = ").append(getTableAlias()).append(".").append(columnReference.getKeyColumn())
					//	Reference ID
					.append(" AND ").append(getTableAlias()).append(".").append("AD_Reference_ID").append(" = ").append(item.getReferenceValueId()).append(")");
					if(columnReference.isIsTranslated()) {
						tableReferences.append(" LEFT OUTER JOIN ")
						.append(columnReference.getTableName()).append("_Trl").append(" ").append(getTableAliasTrl()).append(" ON (")
						.append(getQueryReferenceColumnNameTrl("AD_Ref_List_ID")).append(" = ").append(getQueryReferenceColumnName("AD_Ref_List_ID"))
						//	Reference ID
						.append(getLanguageCriteria(language)).append(")");
					}
				} else if (item.getReferenceId() == DisplayType.Location
						|| item.getReferenceId() == DisplayType.Account
						|| item.getReferenceId() == DisplayType.Locator
						|| item.getReferenceId() == DisplayType.PAttribute
					) {
					ColumnReference columnReference = ColumnReference.getColumnReferenceSpecial(item.getReferenceId());
					if(columnReference != null) {
						addTableAlias();
						StringBuffer displayColumnValue = new StringBuffer();
						if(columnReference.isIsValueDisplayed()) {
							displayColumnValue.append(getQueryReferenceColumnName("Value"));
						}
						if(displayColumnValue.length() > 0) {
							displayColumnValue.append("|| '_' ||");
						}
						displayColumnValue.append(getQueryReferenceColumnName(columnReference.getDisplayColumn()));
						if(query.length() > 0) {
							query.append(", ");
						}
						columnName = displayColumnValue.toString();
						query.append("(").append(displayColumnValue).append(")");
						alias = getDisplayColumnName(item);
						query.append(" AS ").append(alias);
						columns.add(PrintFormatColumn.newInstance(item).withDisplayValue(true).withColumnNameAlias(getDisplayColumnName(item)));
						//	Add JOIN
						if(item.isMandatory()) {
							tableReferences.append(" INNER JOIN ");
						} else {
							tableReferences.append(" LEFT OUTER JOIN ");
						}
						tableReferences.append(columnReference.getTableName()).append(" ").append(getTableAlias()).append(" ON (")
						.append(getQueryReferenceColumnName(columnReference.getKeyColumn())).append("=").append(getQueryColumnName(item.getColumnName())).append(")");
					}
				}
				//	For Order By
				if(item.isOrderBy()) {
					if(!Util.isEmpty(alias)) {
						if(orderBy.length() > 0) {
							orderBy.append(", ");
						}
						orderBy.append(alias);
						if(item.isDesc()) {
							orderBy.append(" DESC");
						}
					}
				}
			}
		});
		if(query.length() > 0) {
			query.insert(0, "SELECT ");
			query.append(" FROM ").append(getTableName());
			if(tableReferences.length() > 0) {
				query.append(tableReferences);
			}
		}
		//	Return definition
		return QueryDefinition.newInstance()
				.withQuery(query.toString())
				.withOrderBy(orderBy.toString())
				.withColumns(columns);
	}
	
	private String getQueryColumnName(String columnName) {
		return getTableName() + "." + columnName;
	}
	
	private String getQueryReferenceColumnName(String columnName) {
		return getTableAlias() + "." + columnName;
	}
	
	private String getQueryReferenceColumnNameTrl(String columnName) {
		return getTableAliasTrl() + "." + columnName;
	}
	
	private String getLanguageCriteria(Language language) {
		if(!Env.isBaseLanguage(language, "AD_Ref_List")) {
			return " AND " + getTableAliasTrl() + ".AD_Language = '" + language.getAD_Language() + "'";
		}
		return "";
	}
	
	private String getTableAlias() {
		return "t" + aliasNumber;
	}
	
	private String getTableAliasTrl() {
		return "tt" + aliasNumber;
	}
	
	private void addTableAlias() {
		aliasNumber ++;
	}
	
	private void clearTableAlias() {
		aliasNumber = 0;
	}
	
	private String getDisplayColumnName(PrintFormatItem item) {
		return item.getColumnName() + "_" + item.getPrintFormatItemId() + "_DisplayValue";
	}

	@Override
	public String toString() {
		return "PrintFormat [name=" + name + ", description=" + description + ", printFormatId=" + printFormatId
				+ ", reportView=" + reportView + ", tableId=" + tableId + ", isSummary=" + isSummary + ", items="
				+ items + "]";
	}
}
