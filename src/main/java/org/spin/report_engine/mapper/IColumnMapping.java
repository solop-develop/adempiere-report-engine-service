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
package org.spin.report_engine.mapper;

import java.sql.ResultSet;

import org.compiere.util.Language;
import org.spin.report_engine.data.Cell;
import org.spin.report_engine.format.PrintFormatColumn;
import org.spin.report_engine.format.PrintFormatItem;

/**
 * This interface represent a contract of column mapper, you can implement your own mapper based on this format
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public interface IColumnMapping {
	
	/**
	 * Process Cell Value from query
	 * @param printFormatLine
	 * @param column
	 * @param language
	 * @param resultSet
	 * @param cell
	 */
	public void processValue(PrintFormatItem printFormatLine, PrintFormatColumn column, Language language, ResultSet resultSet, Cell cell);
}
