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
	
	public Cell withValue(Object value) {
		this.value = value;
		return this;
	}
}
