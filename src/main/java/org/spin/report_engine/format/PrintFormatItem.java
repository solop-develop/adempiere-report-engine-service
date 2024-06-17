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
import org.compiere.model.MColumn;
import org.compiere.print.MPrintFormatItem;
import org.compiere.util.DisplayType;
import org.compiere.util.Util;

/**
 * Print Format Line Representation
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class PrintFormatItem {
	
	private int printFormatItemId;
	private int printFormatChildId;
	private String name;
	private String printText;
	private int sequence;
	private int sortSequence;
	private String printFormatType;
	private String fieldAlignmentType;
	private String barcodeType;
	private String formatPattern;
	private boolean isSuppressNull;
	private boolean isSuppressRepeats;
	private boolean isOrderBy;
	private boolean isDesc;
	private boolean isGroupBy;
	private boolean isSummarized;
	private boolean isCounted;
	private boolean isMinCalc;
	private boolean isAveraged;
	private boolean isDeviationCalc;
	private boolean isHideGrandTotal;
	private boolean isMaxCalc;
	private boolean isVarianceCalc;
	private boolean isPrinted;
	private boolean isActive;
	//	Column attributes
	private String columnName;
	private int columnId;
	private int referenceId;
	private int referenceValueId;
	private boolean isKey;
	private boolean isParent;
	private boolean isMandatory;
	private boolean isVirtualColumn;
	private String columnSql;
	private String mappingClassName;
	
	
	/** PrintFormatType AD_Reference_ID=255 */
	public static final int PRINTFORMATTYPE_AD_Reference_ID=255;
	/** Field = F */
	public static final String PRINTFORMATTYPE_Field = "F";
	/** Text = T */
	public static final String PRINTFORMATTYPE_Text = "T";
	/** Print Format = P */
	public static final String PRINTFORMATTYPE_PrintFormat = "P";
	/** Image = I */
	public static final String PRINTFORMATTYPE_Image = "I";
	/** Rectangle = R */
	public static final String PRINTFORMATTYPE_Rectangle = "R";
	/** Line = L */
	public static final String PRINTFORMATTYPE_Line = "L";
	
	/** BarcodeType AD_Reference_ID=377 */
	public static final int BARCODETYPE_AD_Reference_ID=377;
	/** Codabar 2 of 7 linear = 2o9 */
	public static final String BARCODETYPE_Codabar2Of7Linear = "2o9";
	/** Code 39  3 of 9 linear w/o Checksum = 3o9 */
	public static final String BARCODETYPE_Code393Of9LinearWOChecksum = "3o9";
	/** Codeabar linear = COD */
	public static final String BARCODETYPE_CodeabarLinear = "COD";
	/** Code 128 dynamically switching = C28 */
	public static final String BARCODETYPE_Code128DynamicallySwitching = "C28";
	/** Code 128 A character set = 28A */
	public static final String BARCODETYPE_Code128ACharacterSet = "28A";
	/** Code 128 B character set = 28B */
	public static final String BARCODETYPE_Code128BCharacterSet = "28B";
	/** Code 128 C character set = 28C */
	public static final String BARCODETYPE_Code128CCharacterSet = "28C";
	/** Code 39 linear with Checksum = C39 */
	public static final String BARCODETYPE_Code39LinearWithChecksum = "C39";
	/** EAN 128 = E28 */
	public static final String BARCODETYPE_EAN128 = "E28";
	/** Global Trade Item No GTIN UCC/EAN 128 = GTN */
	public static final String BARCODETYPE_GlobalTradeItemNoGTINUCCEAN128 = "GTN";
	/** Codabar Monarch linear = MON */
	public static final String BARCODETYPE_CodabarMonarchLinear = "MON";
	/** Codabar NW-7 linear = NW7 */
	public static final String BARCODETYPE_CodabarNW_7Linear = "NW7";
	/** PDF417 two dimensional = 417 */
	public static final String BARCODETYPE_PDF417TwoDimensional = "417";
	/** SCC-14 shipping code UCC/EAN 128 = C14 */
	public static final String BARCODETYPE_SCC_14ShippingCodeUCCEAN128 = "C14";
	/** Shipment ID number UCC/EAN 128 = SID */
	public static final String BARCODETYPE_ShipmentIDNumberUCCEAN128 = "SID";
	/** UCC 128 = U28 */
	public static final String BARCODETYPE_UCC128 = "U28";
	/** Code 39 USD3 with Checksum = US3 */
	public static final String BARCODETYPE_Code39USD3WithChecksum = "US3";
	/** Codabar USD-4 linear = US4 */
	public static final String BARCODETYPE_CodabarUSD_4Linear = "US4";
	/** US Postal Service UCC/EAN 128 = USP */
	public static final String BARCODETYPE_USPostalServiceUCCEAN128 = "USP";
	/** SSCC-18 number UCC/EAN 128 = C18 */
	public static final String BARCODETYPE_SSCC_18NumberUCCEAN128 = "C18";
	/** Code 39 USD3 w/o Checksum = us3 */
	public static final String BARCODETYPE_Code39USD3WOChecksum = "us3";
	/** Code 39  3 of 9 linear with Checksum = 3O9 */
	public static final String BARCODETYPE_Code393Of9LinearWithChecksum = "3O9";
	/** Code 39 linear w/o Checksum = c39 */
	public static final String BARCODETYPE_Code39LinearWOChecksum = "c39";
	/** EAN 13 = E13 */
	public static final String BARCODETYPE_EAN13 = "E13";
	/** Quick Response Code = QRC */
	public static final String BARCODETYPE_QuickResponseCode = "QRC";
	
	private PrintFormatItem(MPrintFormatItem printFormatItem) {
		name = printFormatItem.get_Translation(I_AD_PrintFormatItem.COLUMNNAME_Name);
		printText = printFormatItem.get_Translation(I_AD_PrintFormatItem.COLUMNNAME_PrintName);
		sequence = printFormatItem.getSeqNo();
		sortSequence = printFormatItem.getSortNo();
		printFormatItemId = printFormatItem.getAD_PrintFormatItem_ID();
		printFormatChildId = printFormatItem.getAD_PrintFormatChild_ID();
		printFormatType = printFormatItem.getPrintFormatType();
		fieldAlignmentType = printFormatItem.getFieldAlignmentType();
		barcodeType = printFormatItem.getBarcodeType();
		isSuppressNull = printFormatItem.isSuppressNull();
		isSuppressRepeats = printFormatItem.isSuppressRepeats();
		isOrderBy = printFormatItem.isOrderBy();
		isDesc = printFormatItem.isDesc();
		isGroupBy = printFormatItem.isGroupBy();
		isSummarized = printFormatItem.isSummarized();
		isCounted = printFormatItem.isCounted();
		isMinCalc = printFormatItem.isMinCalc();
		isAveraged = printFormatItem.isAveraged();
		isDeviationCalc = printFormatItem.isDeviationCalc();
		isHideGrandTotal = printFormatItem.isHideGrandTotal();
		isMaxCalc = printFormatItem.isMaxCalc();
		isVarianceCalc = printFormatItem.isVarianceCalc();
		isPrinted = printFormatItem.isPrinted();
		isActive = printFormatItem.isActive();
		if(printFormatItem.getPrintFormatType().equals(PRINTFORMATTYPE_Field) && printFormatItem.getAD_Column_ID() > 0) {
			MColumn column = MColumn.get(printFormatItem.getCtx(), printFormatItem.getAD_Column_ID());
			columnName = column.getColumnName();
			columnId = column.getAD_Column_ID();
			referenceId = column.getAD_Reference_ID();
			referenceValueId = column.getAD_Reference_Value_ID();
			formatPattern = column.getFormatPattern();
			isKey = column.isKey();
			isParent = column.isParent();
			isMandatory = column.isMandatory();
			columnSql = column.getColumnSQL();
			isVirtualColumn = !Util.isEmpty(column.getColumnSQL());
		} else {
			referenceId = DisplayType.String;
		}
		if(!Util.isEmpty(printFormatItem.getFormatPattern())) {
			formatPattern = printFormatItem.getFormatPattern();
		}
		mappingClassName = printFormatItem.get_ValueAsString("MappingClassName");
	}
	
	public static PrintFormatItem newInstance(MPrintFormatItem printFormatItem) {
		return new PrintFormatItem(printFormatItem);
	}

	public String getMappingClassName() {
		return mappingClassName;
	}

	public PrintFormatItem withMappingClassName(String mappingClassName) {
		this.mappingClassName = mappingClassName;
		return this;
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

	public int getPrintFormatItemId() {
		return printFormatItemId;
	}

	public int getPrintFormatChildId() {
		return printFormatChildId;
	}

	public int getSortSequence() {
		return sortSequence;
	}
	
	public PrintFormatItem withSortSequence(int sortSequence) {
		this.sortSequence = sortSequence;
		return this;
	}

	public String getPrintFormatType() {
		return printFormatType;
	}

	public String getFieldAlignmentType() {
		return fieldAlignmentType;
	}

	public String getBarcodeType() {
		return barcodeType;
	}

	public boolean isSuppressNull() {
		return isSuppressNull;
	}

	public boolean isSuppressRepeats() {
		return isSuppressRepeats;
	}

	public boolean isOrderBy() {
		return isOrderBy;
	}

	public boolean isDesc() {
		return isDesc;
	}

	public boolean isGroupBy() {
		return isGroupBy;
	}

	public boolean isSummarized() {
		return isSummarized;
	}

	public boolean isCounted() {
		return isCounted;
	}

	public boolean isMinCalc() {
		return isMinCalc;
	}

	public boolean isAveraged() {
		return isAveraged;
	}

	public boolean isDeviationCalc() {
		return isDeviationCalc;
	}

	public boolean isHideGrandTotal() {
		return isHideGrandTotal;
	}

	public boolean isMaxCalc() {
		return isMaxCalc;
	}

	public boolean isVarianceCalc() {
		return isVarianceCalc;
	}

	public boolean isPrinted() {
		return isPrinted;
	}

	public boolean isActive() {
		return isActive;
	}

	public String getFormatPattern() {
		return formatPattern;
	}

	public String getColumnName() {
		return columnName;
	}

	public int getColumnId() {
		return columnId;
	}

	public int getReferenceId() {
		return referenceId;
	}

	public int getReferenceValueId() {
		return referenceValueId;
	}

	public boolean isKey() {
		return isKey;
	}

	public boolean isParent() {
		return isParent;
	}
	
	public boolean isVirtualColumn() {
		return isVirtualColumn;
	}

	public String getColumnSql() {
		return columnSql;
	}

	public boolean isMandatory() {
		return isMandatory;
	}

	@Override
	public String toString() {
		return "PrintFormatItem [name=" + name + ", printText=" + printText + ", sequence=" + sequence + "]";
	}
}
