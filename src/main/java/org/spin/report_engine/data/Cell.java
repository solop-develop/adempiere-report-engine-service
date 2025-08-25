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
package org.spin.report_engine.data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Cell Information can be used to represent the data and some attributes like:
 * <li>Color: A color definintion
 * <li>Style: Represent the style for it
 * <li>Value: Represent the cell value
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class Cell {

	private String color;
	private String style;
	private Object value;
	private String tableName;
	private String displayValue;
	/**	Function	*/
	private SummaryFunction function;

	private Cell() {
		
	}

	public static Cell newInstance() {
		return new Cell();
	}

	public String getColor() {
		return color;
	}

	public Cell withColor(String color) {
		this.color = color;
		return this;
	}

	public String getStyle() {
		return style;
	}

	public Cell withStyle(String style) {
		this.style = style;
		return this;
	}

	public Object getValue() {
		return value;
	}

	public String getTableName() {
		return tableName;
	}

	public Cell withTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	/**
	 * 	Get Function Value
	 * 	@return length or numeric value
	 */
	public BigDecimal getFunctionValue() {
		if (value == null)
			return Env.ZERO;

		//	Numbers - return number value
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}
		if (value instanceof Number) {
			return BigDecimal.valueOf(((Number) value).doubleValue());
		}

		//	Boolean - return 1 for true 0 for false
		if (value instanceof Boolean) {
			if (((Boolean)value).booleanValue()) {
				return Env.ONE;
			}
			return Env.ZERO;
		}

		//	Return Length
		String s = value.toString();
		return new BigDecimal(s.length());
	}	//	getFunctionValue

	public Cell withValue(Object value) {
		this.value = value;
		return this;
	}

	public String getCompareValue() {
		if(value instanceof Timestamp && value != null) {
			return new SimpleDateFormat("yyyy-MM-dd").format((Timestamp) value);
		}
		if(Util.isEmpty(displayValue)) {
			if(value != null) {
				return String.valueOf(value);
			}
			return "";
		}
		return displayValue;
	}

	public String getDisplayValue() {
		if(Util.isEmpty(displayValue)) {
			return "";
		}
		return displayValue;
	}

	public Cell withDisplayValue(String displayValue) {
		this.displayValue = displayValue;
		return this;
	}

	public Cell withFunction(SummaryFunction function) {
		this.function = function;
		return this;
	}

	public Cell withSumDisplayValue(String displayValue) {
		if(function == null) {
			return this;
		}
		function.setDisplayValue(SummaryFunction.F_SUM, displayValue);
		return this;
	}

	public Cell withMeanDisplayValue(String displayValue) {
		if(function == null) {
			return this;
		}
		function.setDisplayValue(SummaryFunction.F_MEAN, displayValue);
		return this;
	}

	public Cell withCountDisplayValue(String displayValue) {
		if(function == null) {
			return this;
		}
		function.setDisplayValue(SummaryFunction.F_COUNT, displayValue);
		return this;
	}

	public Cell withMinimumDisplayValue(String displayValue) {
		if(function == null) {
			return this;
		}
		function.setDisplayValue(SummaryFunction.F_MIN, displayValue);
		return this;
	}

	public Cell withMaximumDisplayValue(String displayValue) {
		if(function == null) {
			return this;
		}
		function.setDisplayValue(SummaryFunction.F_MAX, displayValue);
		return this;
	}

	public Cell withVarianceDisplayValue(String displayValue) {
		if(function == null) {
			return this;
		}
		function.setDisplayValue(SummaryFunction.F_VARIANCE, displayValue);
		return this;
	}

	public Cell withDeviationDisplayValue(String displayValue) {
		if(function == null) {
			return this;
		}
		function.setDisplayValue(SummaryFunction.F_DEVIATION, displayValue);
		return this;
	}

	public String getSumDisplayValue() {
		if(function == null) {
			return null;
		}
		return function.getDisplayValue(SummaryFunction.F_SUM);
	}

	public String getMeanDisplayValue() {
		if(function == null) {
			return null;
		}
		return function.getDisplayValue(SummaryFunction.F_MEAN);
	}

	public String getCountDisplayValue() {
		if(function == null) {
			return null;
		}
		return function.getDisplayValue(SummaryFunction.F_COUNT);
	}

	public String getMinimumDisplayValue() {
		if(function == null) {
			return null;
		}
		return function.getDisplayValue(SummaryFunction.F_MIN);
	}

	public String getMaximumDisplayValue() {
		if(function == null) {
			return null;
		}
		return function.getDisplayValue(SummaryFunction.F_MAX);
	}

	public String getVarianceDisplayValue() {
		if(function == null) {
			return null;
		}
		return function.getDisplayValue(SummaryFunction.F_VARIANCE);
	}

	public String getDeviationDisplayValue() {
		if(function == null) {
			return null;
		}
		return function.getDisplayValue(SummaryFunction.F_DEVIATION);
	}

	public BigDecimal getSum() {
		if(function == null) {
			return null;
		}
		return function.getValue(SummaryFunction.F_SUM);
	}

	public BigDecimal getMean() {
		if(function == null) {
			return null;
		}
		return function.getValue(SummaryFunction.F_MEAN);
	}

	public BigDecimal getCount() {
		if(function == null) {
			return null;
		}
		return function.getValue(SummaryFunction.F_COUNT);
	}

	public BigDecimal getMinimum() {
		if(function == null) {
			return null;
		}
		return function.getValue(SummaryFunction.F_MIN);
	}

	public BigDecimal getMaximum() {
		if(function == null) {
			return null;
		}
		return function.getValue(SummaryFunction.F_MAX);
	}

	public BigDecimal getVariance() {
		if(function == null) {
			return null;
		}
		return function.getValue(SummaryFunction.F_VARIANCE);
	}

	public BigDecimal getDeviation() {
		if(function == null) {
			return null;
		}
		return function.getValue(SummaryFunction.F_DEVIATION);
	}

	@Override
	public String toString() {
		return "Cell [value=" + value + ", displayValue=" + displayValue + "]";
	}

	@Override
	public boolean equals(Object object) {
		if(value == null && object == null) {
			return false;
		}
		if(value == null && object != null) {
			return false;
		}
		if(value != null && object == null) {
			return false;
		}
		Cell cell = (Cell) object;
		return value.equals(cell.getValue());
	}

}
