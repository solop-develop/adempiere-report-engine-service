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

import org.adempiere.core.domains.models.I_AD_PrintFormatItem;
import org.compiere.print.MPrintFormatItem;

/**
 * Print Format Line Representation
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class PrintFormatItem {
	
	private String name;
	private String printText;
	private int sequence;
	
	private PrintFormatItem(MPrintFormatItem printFormatItem) {
		name = printFormatItem.getName();
		printText = printFormatItem.get_Translation(I_AD_PrintFormatItem.COLUMNNAME_PrintName);
		sequence = printFormatItem.getAD_PrintFormat_ID();
	}
	
	public static PrintFormatItem newInstance(MPrintFormatItem printFormatItem) {
		return new PrintFormatItem(printFormatItem);
	}

	public String getName() {
		return name;
	}

	public String getPrintText() {
		return printText;
	}

	public int getSequence() {
		return sequence;
	}

	@Override
	public String toString() {
		return "PrintFormatItem [name=" + name + ", printText=" + printText + ", sequence=" + sequence + "]";
	}
}
