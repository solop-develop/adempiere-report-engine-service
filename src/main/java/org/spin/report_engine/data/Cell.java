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
	
	/**
	 * 	Get Function Value
	 * 	@return length or numeric value
	 */
	public BigDecimal getFunctionValue() {
		if (value == null)
			return Env.ZERO;

		//	Numbers - return number value
		if (value instanceof BigDecimal)
			return (BigDecimal)value;
		if (value instanceof Number)
			return BigDecimal.valueOf(((Number)value).doubleValue());

		//	Boolean - return 1 for true 0 for false
		if (value instanceof Boolean)
		{
			if (((Boolean)value).booleanValue())
				return Env.ONE;
			else
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

	public String getDisplayValue() {
		if(Util.isEmpty(displayValue)) {
			if(value != null) {
				return String.valueOf(value);
			}
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
