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

import org.spin.report_engine.format.PrintFormatItem;

/**
 * This class have all need for manage row summary and groups
 * <li>Color: A color definition
 * <li>Style: Represent the style for it
 * <li>Code: Represent the unique code or ID for column
 * <li>Title: Represent the column title
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ColumnInfo {
	private String color;
	private String style;
	private String code;
	private String title;
	private int printFormatItemId;
	private int displayTypeId;
	private int sequence;
	private boolean isGroupColumn;
	
	private ColumnInfo(PrintFormatItem item) {
		this.title = item.getPrintText();
		this.printFormatItemId = item.getPrintFormatItemId();
		this.code = String.valueOf(item.getPrintFormatItemId());
		this.displayTypeId = item.getReferenceId();
		this.sequence = item.getSequence();
		this.isGroupColumn = item.isGroupBy();
	}
	
	public static ColumnInfo newInstance(PrintFormatItem item) {
		return new ColumnInfo(item);
	}
	
	public int getDisplayTypeId() {
		return displayTypeId;
	}

	public ColumnInfo withDisplayTypeId(int displayTypeId) {
		this.displayTypeId = displayTypeId;
		return this;
		
	}

	public String getColor() {
		return color;
	}
	
	public ColumnInfo withColor(String color) {
		this.color = color;
		return this;
	}
	
	public String getStyle() {
		return style;
	}
	
	public ColumnInfo withStyle(String style) {
		this.style = style;
		return this;
	}
	
	public String getCode() {
		return code;
	}
	
	public ColumnInfo withCode(String code) {
		this.code = code;
		return this;
	}
	
	public String getTitle() {
		return title;
	}
	
	public ColumnInfo withTitle(String title) {
		this.title = title;
		return this;
	}

	public int getPrintFormatItemId() {
		return printFormatItemId;
	}

	public ColumnInfo withPrintFormatItemId(int printFormatItemId) {
		this.printFormatItemId = printFormatItemId;
		return this;
	}

	public int getSequence() {
		return sequence;
	}

	public boolean isGroupColumn() {
		return isGroupColumn;
	}

	@Override
	public String toString() {
		return "ColumnInfo [color=" + color + ", style=" + style + ", code=" + code + ", title=" + title + "]";
	}
}
