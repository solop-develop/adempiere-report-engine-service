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
import java.util.Optional;

import org.compiere.model.MRole;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.service.grpc.util.db.OperatorUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.value.ValueManager;

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
	private List<PrintFormatColumn> queryColumns;
	private String whereClause;
	private String completeQuery;
	private String completeQueryCount;
	private int limit;
	private int offset;
	private int instanceId;
	public static final int NO_LIMIT = -1;
	private String tableName;
	
	private QueryDefinition() {
		conditions = new ArrayList<Filter>();
		columns = new ArrayList<PrintFormatColumn>();
		queryColumns = new ArrayList<PrintFormatColumn>();
		parameters = new ArrayList<Object>();
	}
	
	public static QueryDefinition newInstance() {
		return new QueryDefinition();
	}

	public String getQuery() {
		return query;
	}

	public String getTableName() {
		return tableName;
	}

	public QueryDefinition withTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public QueryDefinition withQuery(String query) {
		this.query = query;
		return this;
	}
	
	public int getInstanceId() {
		return instanceId;
	}

	public QueryDefinition withInstanceId(int instanceId) {
		this.instanceId = instanceId;
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
	
	public QueryDefinition withLimit(int limit, int offset) {
		this.limit = limit;
		this.offset = offset;
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
	
	public List<PrintFormatColumn> getQueryColumns() {
		return queryColumns;
	}

	public QueryDefinition withQueryColumns(List<PrintFormatColumn> queryColumns) {
		this.queryColumns = queryColumns;
		return this;
	}

	public List<Object> getParameters() {
		return parameters;
	}

	public String getWhereClause() {
		return this.whereClause;
	}

	public QueryDefinition withWhereClause(String whereClause) {
		this.whereClause = whereClause;
		return this;
	}

	public String getCompleteQuery() {
		return completeQuery;
	}

	public String getCompleteQueryCount() {
		return completeQueryCount;
	}

	public QueryDefinition withCompleteQueryCount(String completeQueryCount) {
		this.completeQueryCount = completeQueryCount;
		return this;
	}

	public QueryDefinition withCompleteQuery(String completeQuery) {
		this.completeQuery = completeQuery;
		return this;
	}

	public QueryDefinition buildQuery() {
		// Add Query columns
		String query = getQuery();
		// Add Where restriction
		// TODO: Add 1=1 to remove `if (whereClause.length() > 0)` and change stream with parallelStream
		StringBuffer whereClause = new StringBuffer();
		getConditions().stream()
			.filter(condition -> !Util.isEmpty(condition.getColumnName(), true))
			.forEach(condition -> {
				Optional<PrintFormatColumn> maybeColumn = getColumns()
					.stream()
					.filter(column -> {
						final String conditionColumnName = condition.getColumnName();
						return conditionColumnName.equals(column.getColumnName())
							|| conditionColumnName.equals(column.getColumnName() + "_To");
					})
					.sorted(Comparator.comparing(PrintFormatColumn::getColumnNameAlias).reversed())
					.findFirst()
				;
				if(maybeColumn.isPresent()) {
					if (whereClause.length() > 0) {
						whereClause.append(" AND ");
					}
					PrintFormatColumn column = maybeColumn.get();
					condition.setColumnName(column.getColumnNameAlias());
					String restriction = getRestrictionByOperator(condition, column.getReferenceId());
					whereClause.append(restriction);
				}
		});
		withWhereClause(whereClause.toString());
		if(!Util.isEmpty(getWhereClause(), true)) {
			query = query + " WHERE " + getWhereClause();
		}
		//	Add SQL Access
		if(!tableName.equals("T_Report")) {
			query = MRole.getDefault(Env.getCtx(), false).addAccessSQL(
					query, getTableName(), MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO);
		}
		StringBuffer completeQuery = new StringBuffer(query);
		StringBuffer completeQueryWithoutLimit = new StringBuffer(query);
		//	Add Limit records
		if(this.limit != NO_LIMIT) {
			if(this.limit == 0) {
				withLimit(100, 0);
			}

			StringBuffer limitClause = new StringBuffer()
				// TODO: Implement with use https://github.com/adempiere/adempiere/pull/4142
				// .append(" LIMIT ")
				// .append(this.limit)
				// .append(" OFFSET ")
				// .append(this.offset)
				// .append(completeQueryCount)
			;

			if(!Util.isEmpty(getWhereClause(), true)) {
				limitClause.insert(0, " AND ");
			} else {
				limitClause.insert(0, " WHERE ");
			}
			limitClause.append("ROWNUM <= ").append(this.limit);
			limitClause.append(" AND ROWNUM >= ").append(this.offset);

			completeQuery.append(limitClause.toString());
		}

		// Add Group By
		if(!Util.isEmpty(getGroupBy(), true)) {
			completeQuery.append(" GROUP BY ").append(getGroupBy());
			completeQueryWithoutLimit.append(" GROUP BY ").append(getGroupBy());
		}

		// Add Order By
		if(!Util.isEmpty(getOrderBy(), true)) {
			completeQuery.append(" ORDER BY ").append(getOrderBy());
			completeQueryWithoutLimit.append(" ORDER BY ").append(getOrderBy());
		}

		withCompleteQueryCount(completeQueryWithoutLimit.toString());
		withCompleteQuery(completeQuery.toString());

		return this;
	}
	
	/**
	 * Get sql restriction by operator
	 * @param condition
	 * @param displayType
	 * @param parameters
	 * @return
	 */
	private String getRestrictionByOperator(Filter condition, int displayType) {
		String operatorValue = OperatorUtil.EQUAL;
		if (!Util.isEmpty(condition.getOperator(), true)) {
			operatorValue = condition.getOperator().toLowerCase();
		}
		String sqlOperator = OperatorUtil.convertOperator(condition.getOperator());

		String columnName = condition.getColumnName();
		String sqlValue = "";
		StringBuilder additionalSQL = new StringBuilder();
		//	For IN or NOT IN
		if (operatorValue.equals(OperatorUtil.IN) || operatorValue.equals(OperatorUtil.NOT_IN)) {
			StringBuilder parameterValues = new StringBuilder();
			final String baseColumnName = columnName;
			StringBuilder column_name = new StringBuilder(columnName);

			if (condition.getValues() != null) {
				condition.getValues().forEach(currentValue -> {
					boolean isString = DisplayType.isText(displayType) || currentValue instanceof String;

					if (currentValue == null || (isString && Util.isEmpty((String) currentValue, true))) {
						if (Util.isEmpty(additionalSQL.toString(), true)) {
							additionalSQL.append("(SELECT " + baseColumnName + " WHERE " + baseColumnName + " IS NULL)");
						}
						if (isString) {
							currentValue = "";
						} else {
							// does not add the null value to the filters, another restriction is
							// added only for null values `additionalSQL`.
							return;
						}
					}
					if (parameterValues.length() > 0) {
						parameterValues.append(", ");
					}
					String sqlInValue = "?";
					if (isString) {
						column_name.delete(0, column_name.length());
						column_name.append("UPPER(").append(baseColumnName).append(")");
						sqlInValue = "UPPER(?)";
					}
					parameterValues.append(sqlInValue);

					Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
						displayType,
						currentValue
					);
					this.parameters.add(valueToFilter);
				});
			}

			columnName = column_name.toString();
			if (!Util.isEmpty(parameterValues.toString(), true)) {
				sqlValue = "(" + parameterValues.toString() + ")";
				if (!Util.isEmpty(additionalSQL.toString(), true)) {
					additionalSQL.insert(0, " OR " + columnName + sqlOperator);
				}
			}
		} else if(operatorValue.equals(OperatorUtil.BETWEEN) || operatorValue.equals(OperatorUtil.NOT_BETWEEN)) {
			// List<Object> values = condition.getValues();
			Object valueStartToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getFromValue()
			);
			Object valueEndToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getToValue()
			);

			sqlValue = "";
			if (valueStartToFilter == null) {
				sqlValue = " ? ";
				sqlOperator = OperatorUtil.convertOperator(OperatorUtil.LESS_EQUAL);
				this.parameters.add(valueEndToFilter);
			} else if (valueEndToFilter == null) {
				sqlValue = " ? ";
				sqlOperator = OperatorUtil.convertOperator(OperatorUtil.GREATER_EQUAL);
				this.parameters.add(valueStartToFilter);
			} else {
				sqlValue = " ? AND ? ";
				this.parameters.add(valueStartToFilter);
				this.parameters.add(valueEndToFilter);
			}
		} else if(operatorValue.equals(OperatorUtil.LIKE) || operatorValue.equals(OperatorUtil.NOT_LIKE)) {
			columnName = "UPPER(" + columnName + ")";
			String valueToFilter = ValueManager.validateNull(
				(String) condition.getValue()
			);
			// if (!Util.isEmpty(valueToFilter, true)) {
			// 	if (!valueToFilter.startsWith("%")) {
			// 		valueToFilter = "%" + valueToFilter;
			// 	}
			// 	if (!valueToFilter.endsWith("%")) {
			// 		valueToFilter += "%";
			// 	}
			// }
			// valueToFilter = "UPPPER(" + valueToFilter + ")";
			sqlValue = "'%' || UPPER(?) || '%'";
			this.parameters.add(valueToFilter);
		} else if(operatorValue.equals(OperatorUtil.NULL) || operatorValue.equals(OperatorUtil.NOT_NULL)) {
			;
		} else if (operatorValue.equals(OperatorUtil.EQUAL) || operatorValue.equals(OperatorUtil.NOT_EQUAL)) {
			Object parameterValue = condition.getValue();
			sqlValue = " ? ";

			boolean isString = DisplayType.isText(displayType);
			boolean isEmptyString = isString && Util.isEmpty((String) parameterValue, true);
			if (isString) {
				if (isEmptyString) {
					parameterValue = "";
				} else {
					columnName = "UPPER(" + columnName + ")";
					sqlValue = "UPPER(?)";
				}
			}
			if (parameterValue == null || isEmptyString) {
				additionalSQL.append(" OR ")
					.append(columnName)
					.append(" IS NULL ")
				;
			}

			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				parameterValue
			);
			this.parameters.add(valueToFilter);
		} else {
			// Greater, Greater Equal, Less, Less Equal
			sqlValue = " ? ";

			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getValue()
			);
			this.parameters.add(valueToFilter);
		}

		String rescriction = "(" + columnName + sqlOperator + sqlValue + additionalSQL.toString() + ")";

		return rescriction;
	}

	@Override
	public String toString() {
		return "QueryDefinition [query=" + query + ", groupBy=" + groupBy + ", orderBy=" + orderBy + "]";
	}
}
