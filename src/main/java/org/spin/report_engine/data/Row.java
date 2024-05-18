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

import java.util.HashMap;
import java.util.Map;

/**
 * Row information or representation for Row
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class Row {
	/**	Data for Row	*/
	private Map<Integer, Cell> data;
	
	private Row() {
		data = new HashMap<>();
	}
	
	public static Row newInstance() {
		return new Row();
	}
	
	public Row withCell(int printFormatItemId, Cell cell) {
		data.put(printFormatItemId, cell);
		return this;
	}
	
	public Row withCells(Map<Integer, Cell> cells) {
		data = cells;
		return this;
	}
	
	public Map<Integer, Cell> getData() {
		return data;
	}

	@Override
	public String toString() {
		return "Row [data=" + data + "]";
	}
}
