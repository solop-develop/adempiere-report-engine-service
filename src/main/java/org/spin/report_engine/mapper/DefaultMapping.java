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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.report_engine.data.Cell;
import org.spin.report_engine.format.PrintFormatColumn;
import org.spin.report_engine.format.PrintFormatItem;
import org.spin.service.grpc.util.value.BooleanManager;

/**
 * Default just return the same value of cell
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class DefaultMapping implements IColumnMapping {
	
	public static DefaultMapping newInstance() {
		return new DefaultMapping();
	}
	
	public void processValue(PrintFormatItem printFormatLine, PrintFormatColumn column, Language language, ResultSet resultSet, Cell cell) {
		if(cell == null) {
			return;
		}
		try {
			if(DisplayType.isLookup(column.getReferenceId()) && column.getReferenceId() != DisplayType.List || column.getColumnName().equals("Record_ID")) {
				int valueId = resultSet.getInt(column.getColumnName());
				cell.withValue(valueId);
				if(column.getColumnName().equals("Record_ID")) {
					try {
						String tableName = resultSet.getString("TableName");
						if(!Util.isEmpty(tableName)) {
							cell.withTableName(tableName);
							if(valueId > 0) {
								MTable table = MTable.get(Env.getCtx(), tableName);
								PO entity = table.getPO(valueId, null);
								cell.withDisplayValue(entity.getDisplayValue());
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if(DisplayType.isDate(printFormatLine.getReferenceId())) {
				if(cell.getValue() != null) {
					Timestamp date = (Timestamp) cell.getValue();
					cell.withDisplayValue(DisplayType.getDateFormat(printFormatLine.getReferenceId(), language, printFormatLine.getFormatPattern()).format(date));
				}
			} else if(DisplayType.isNumeric(printFormatLine.getReferenceId())) {
				if(cell.getValue() != null) {
					if(BigDecimal.class.isAssignableFrom(cell.getValue().getClass())) {
						BigDecimal number = (BigDecimal) cell.getValue();
						cell.withDisplayValue(DisplayType.getNumberFormat(printFormatLine.getReferenceId(), language, printFormatLine.getFormatPattern()).format(number));
					}
				}
			} else if(printFormatLine.getReferenceId() == DisplayType.YesNo) {
				if(cell.getValue() != null) {
					if(Boolean.class.isAssignableFrom(cell.getValue().getClass())) {
						boolean booleanValue = (boolean) cell.getValue();
						cell.withDisplayValue(Msg.getMsg(Env.getCtx(), booleanValue ? "Y" : "N"));
					} else if(String.class.isAssignableFrom(cell.getValue().getClass())) {
						boolean booleanValue = BooleanManager.getBooleanFromString(String.valueOf(cell.getValue()));
						cell.withDisplayValue(Msg.getMsg(Env.getCtx(), booleanValue ? "Y" : "N"));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
