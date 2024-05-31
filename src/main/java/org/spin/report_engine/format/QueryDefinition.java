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
import org.compiere.util.DisplayType;
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
	private String whereClause;
	private String completeQuery;
	private int limit; 
	private int offset;
	private int instanceId;
	
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

	public String getCompleteQuery() {
		return completeQuery;
	}

	public QueryDefinition withCompleteQuery(String completeQuery) {
		this.completeQuery = completeQuery;
		return this;
	}

	public QueryDefinition buildQuery() {
		// TODO: Add 1=1 to remove `if (whereClause.length() > 0)` and change stream with parallelStream
		StringBuffer whereClause = new StringBuffer();
		getConditions().stream()
			.filter(condition -> !Util.isEmpty(condition.getColumnName(), true))
			.forEach(condition -> {
				Optional<PrintFormatColumn> maybeColumn = getColumns()
						.stream()
						.filter(column -> column.getColumnName().equals(condition.getColumnName()))
						.sorted(Comparator.comparing(PrintFormatColumn::getColumnNameAlias))
						.findFirst();
				if(maybeColumn.isPresent()) {
					if (whereClause.length() > 0) {
						whereClause.append(" AND ");
					}
					PrintFormatColumn column = maybeColumn.get();
					condition.setColumnName(column.getColumnName());
					String restriction = getRestrictionByOperator(condition, column.getReferenceId());
					whereClause.append(restriction);
				}
		});
		//	Add Limit
		if(limit <= 0) {
			withLimit(100, 0);
		}
		if(getInstanceId() > 0) {
			if (whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("AD_PInstance_ID = ").append(getInstanceId());
		}
		if (whereClause.length() > 0) {
			whereClause.append(" AND ");
		}
		whereClause.append("ROWNUM <= ").append(limit);
		//	Add offset
		if (whereClause.length() > 0) {
			whereClause.append(" AND ");
		}
		whereClause.append("ROWNUM >= ").append(offset);
		//	
		withWhereClause(whereClause.toString());
		StringBuffer completeQuery = new StringBuffer(getQuery());
		if(!Util.isEmpty(getWhereClause())) {
			completeQuery.append(" WHERE ").append(getWhereClause());
		}
		if(!Util.isEmpty(getGroupBy())) {
			completeQuery.append(" GROUP BY ").append(getGroupBy());
		}
		if(!Util.isEmpty(getOrderBy())) {
			completeQuery.append(" ORDER BY ").append(getOrderBy());
		}
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
					parameters.add(valueToFilter);
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
				parameters.add(valueEndToFilter);
			} else if (valueEndToFilter == null) {
				sqlValue = " ? ";
				sqlOperator = OperatorUtil.convertOperator(OperatorUtil.GREATER_EQUAL);
				parameters.add(valueStartToFilter);
			} else {
				sqlValue = " ? AND ? ";
				parameters.add(valueStartToFilter);
				parameters.add(valueEndToFilter);
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
			parameters.add(valueToFilter);
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
			parameters.add(valueToFilter);
		} else {
			// Greater, Greater Equal, Less, Less Equal
			sqlValue = " ? ";

			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getValue()
			);
			parameters.add(valueToFilter);
		}

		String rescriction = "(" + columnName + sqlOperator + sqlValue + additionalSQL.toString() + ")";

		return rescriction;
	}

	@Override
	public String toString() {
		return "QueryDefinition [query=" + query + ", groupBy=" + groupBy + ", orderBy=" + orderBy + "]";
	}
}
