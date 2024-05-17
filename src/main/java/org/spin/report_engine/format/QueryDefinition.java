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

import org.compiere.util.Util;
import org.spin.service.grpc.util.query.Filter;

/**
 * Query Representation
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class QueryDefinition {
	private String query;
	private String groupBy;
	private String orderBy;
	private List<Filter> conditions;
	private List<Object> parameters;
	private List<PrintFormatColumn> columns;
	private String whereClause;
	
	private QueryDefinition() {
		conditions = new ArrayList<Filter>();
		columns = new ArrayList<PrintFormatColumn>();
		parameters = new ArrayList<Object>();
	}
	
	public static QueryDefinition newInstance() {
		return new QueryDefinition();
	}

	public String getQuery() {
		return query;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public QueryDefinition withQuery(String query) {
		this.query = query;
		return this;
	}

	public QueryDefinition withOrderBy(String orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	public String getGroupBy() {
		return groupBy;
	}

	public QueryDefinition withGroupBy(String groupBy) {
		this.groupBy = groupBy;
		return this;
	}
	
	public List<Filter> getConditions() {
		return conditions;
	}

	public QueryDefinition withConditions(List<Filter> conditions) {
		this.conditions = conditions;
		return this;
	}

	public List<PrintFormatColumn> getColumns() {
		return columns;
	}

	public QueryDefinition withColumns(List<PrintFormatColumn> columns) {
		this.columns = columns;
		return this;
	}

	public List<Object> getParameters() {
		return parameters;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public QueryDefinition withWhereClause(String whereClause) {
		this.whereClause = whereClause;
		return this;
	}

	public QueryDefinition getWhereClauseFromCriteria() {
		// TODO: Add 1=1 to remove `if (whereClause.length() > 0)` and change stream with parallelStream
		StringBuffer whereClause = new StringBuffer();
		getConditions().stream()
			.filter(condition -> !Util.isEmpty(condition.getColumnName(), true))
			.forEach(condition -> {
				if (whereClause.length() > 0) {
					whereClause.append(" AND ");
				}
//				int displayTypeId = column.getAD_Reference_ID();
//				// set table alias to column name
//				// TODO: Evaluate support to columnSQL
//				String columnName = tableNameAlias + "." + column.getColumnName();
//				condition.setColumnName(columnName);
//				String restriction = WhereClauseUtil.getRestrictionByOperator(condition, displayTypeId, parameters);

//				whereClause.append(restriction);
		});
		withWhereClause(whereClause.toString());
		return this;
	}

	@Override
	public String toString() {
		return "QueryDefinition [query=" + query + ", groupBy=" + groupBy + ", orderBy=" + orderBy + "]";
	}
}
