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
package org.spin.report_engine.export;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.print.attribute.standard.MediaSizeName;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.hssf.usermodel.HSSFPrintSetup;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.compiere.Adempiere;
import org.compiere.print.MPrintFormat;
import org.compiere.print.MPrintPaper;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.report_engine.data.ColumnInfo;
import org.spin.report_engine.data.ReportInfo;

/**
 * Report Xlsx Representation
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class XlsxExporter implements IReportEngineExporter {

	public static XlsxExporter newInstance() {
		return new XlsxExporter();
	}
	
	private XlsxExporter() {
		workBook = new SXSSFWorkbook(100);
		language = Language.getLoginLanguage();
		dataFormat = workBook.createDataFormat();
	}
	
	private SXSSFWorkbook workBook;
	private MPrintPaper printPaper;
	private Font fontHeader = null;
	private Font fontDefault = null;
	private DataFormat dataFormat;
	private Language language;
	/** Styles cache */
	private Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
	
	private Language getLanguage() {
		return language;
	}
	
	private Sheet createSheet() {
		SXSSFSheet sheet = (SXSSFSheet) workBook.createSheet();
		formatPage(sheet);
		sheet.trackAllColumnsForAutoSizing();
		createHeaderFooter(sheet);
		return sheet;
	}
	
	private MPrintPaper getPrintPaper() {
		return printPaper;
	}
	
	private void setReportInfo(ReportInfo reportInfo) {
		MPrintFormat printFormat = new MPrintFormat(Env.getCtx(), reportInfo.getPrintFormatId(), null);
		printPaper = MPrintPaper.get(printFormat.getAD_PrintPaper_ID());
	}
	
	private void formatPage(Sheet sheet) {
		// Set paper size:
		short paperSize = -1;
		MediaSizeName mediaSizeName = getPrintPaper().getMediaSize().getMediaSizeName();
		if (MediaSizeName.NA_LETTER.equals(mediaSizeName)) {
			paperSize = PrintSetup.LETTER_PAPERSIZE;
		}
		else if (MediaSizeName.NA_LEGAL.equals(mediaSizeName)) {
		  	paperSize = PrintSetup.LEGAL_PAPERSIZE;
		}
		else if (MediaSizeName.EXECUTIVE.equals(mediaSizeName)) {
			paperSize = PrintSetup.EXECUTIVE_PAPERSIZE;
		}
		else if (MediaSizeName.ISO_A4.equals(mediaSizeName)) {
			paperSize = PrintSetup.A4_PAPERSIZE;
		}
		else if (MediaSizeName.ISO_A5.equals(mediaSizeName)) {
			paperSize = PrintSetup.A5_PAPERSIZE;
		}
		else if (MediaSizeName.NA_NUMBER_10_ENVELOPE.equals(mediaSizeName)) {
			paperSize = PrintSetup.ENVELOPE_10_PAPERSIZE;
		}
		else if (MediaSizeName.MONARCH_ENVELOPE.equals(mediaSizeName)) {
			paperSize = HSSFPrintSetup.ENVELOPE_MONARCH_PAPERSIZE;
		}
		if (paperSize != -1) {
			sheet.getPrintSetup().setPaperSize(paperSize);
		}
		//
		// Set Landscape/Portrait:
		sheet.getPrintSetup().setLandscape(getPrintPaper().isLandscape());
		//
		// Set Paper Margin:
		sheet.setMargin(PageMargin.TOP, ((double) getPrintPaper().getMarginTop()) /72);
		sheet.setMargin(PageMargin.RIGHT, ((double) getPrintPaper().getMarginRight()) / 72);
		sheet.setMargin(PageMargin.LEFT, ((double) getPrintPaper().getMarginLeft()) / 72);
		sheet.setMargin(PageMargin.BOTTOM, ((double) getPrintPaper().getMarginBottom()) / 72);
	}
	
	private void createHeaderFooter(Sheet sheet) {
		// Sheet Header
		Header header = sheet.getHeader();
		header.setRight("Page &P of &N");
		// Sheet Footer
		Footer footer = sheet.getFooter();
		footer.setLeft(Adempiere.ADEMPIERE_R);
		footer.setCenter(Env.getHeader(Env.getCtx(), 0));
		Timestamp now = new Timestamp(System.currentTimeMillis());
		footer.setRight(DisplayType.getDateFormat(DisplayType.DateTime, getLanguage()).format(now));
	}
	
	@Override
	public String export(ReportInfo reportInfo) {
		setReportInfo(reportInfo);
		//	Create Sheet with report info name
		Sheet sheet = createSheet();
		//	create header
		Row headerRow = sheet.createRow(0);
		List<ColumnInfo> columns = reportInfo.getColumns();
		IntStream.range(0, columns.size())
		.forEach(cellNumber -> {
			Cell sheetCell = headerRow.createCell(cellNumber);
			ColumnInfo columnInfo = columns.get(cellNumber);
			String value = Util.stripDiacritics(columnInfo.getTitle());
			sheetCell.setCellValue(sheet.getWorkbook().getCreationHelper().createRichTextString(value));
			//
			CellStyle style = getHeaderStyle(cellNumber);
			sheetCell.setCellStyle(style);
		});
		//	Export content
		List<org.spin.report_engine.data.Row> rows = reportInfo.getCompleteRows();
		IntStream.range(0, rows.size()).forEach(rowNumber -> {
			Row sheetRow = sheet.createRow(rowNumber + 1);
			IntStream.range(0, columns.size())
			.forEach(columnNumber -> {
				Cell sheetCell = sheetRow.createCell(columnNumber);
				ColumnInfo columnInfo = columns.get(columnNumber);
				org.spin.report_engine.data.Row rowValue = rows.get(rowNumber);
				org.spin.report_engine.data.Cell cell = rowValue.getCell(columnInfo.getPrintFormatItemId());
				int displayType = columnInfo.getDisplayTypeId();
				Object obj = cell.getValue();
				if(obj != null) {
					if (DisplayType.isDate(displayType)) {
						Timestamp value = (Timestamp)obj;
						sheetCell.setCellValue(value);
					} else if (DisplayType.isNumeric(displayType)) {
						double value = 0;
						if (obj instanceof Number) {
							value = ((Number)obj).doubleValue();
						}
						sheetCell.setCellValue(value);
					} else if (DisplayType.YesNo == displayType) {
						String value = Util.stripDiacritics(cell.getDisplayValue());
						sheetCell.setCellValue(sheet.getWorkbook().getCreationHelper().createRichTextString(value));
					} else {
						String value = Util.stripDiacritics(cell.getDisplayValue());
						sheetCell.setCellValue(sheet.getWorkbook().getCreationHelper().createRichTextString(value));
					}
				}
				//
				CellStyle style = getStyle(rowNumber, columnNumber, rowValue.isSummaryRow(), displayType, null);
				sheetCell.setCellStyle(style);
			});
		});
		IntStream.range(0, columns.size())
		.forEach(columnNumber -> sheet.autoSizeColumn(columnNumber));
		sheet.createFreezePane(0, 1);
		return writeFile();
	}
	
	private CellStyle getHeaderStyle(int col) {
		String key = "header-"+col;
		CellStyle cs_header = styles.get(key);
		if (cs_header == null) {
			Font font_header = getHeaderFont();
			cs_header = workBook.createCellStyle();
			cs_header.setFont(font_header);
			cs_header.setBorderLeft(BorderStyle.THIN);
			cs_header.setBorderTop(BorderStyle.THIN);
			cs_header.setBorderRight(BorderStyle.THIN);
			cs_header.setBorderBottom(BorderStyle.THIN);
			cs_header.setDataFormat(workBook.createDataFormat().getFormat("text"));
			cs_header.setWrapText(true);
			styles.put(key, cs_header);
		}
		return cs_header;
	}
	
	private CellStyle getStyle(int row, int col, boolean summaryRow, int displayType, String formatPattern) {
		String key = "cell-" + col + "-" + displayType + "-" + summaryRow;
		CellStyle cs = styles.get(key);
		if (cs == null) {
			boolean isHighlightNegativeNumbers = true;
			cs = workBook.createCellStyle();
			Font font = summaryRow? getCellSummaryFont(): getCellFont();
			cs.setFont(font);
			// Border
			cs.setBorderLeft(BorderStyle.THIN);
			cs.setBorderTop(BorderStyle.THIN);
			cs.setBorderRight(BorderStyle.THIN);
			cs.setBorderBottom(BorderStyle.THIN);
			cs.setWrapText(true);
			//
			if (DisplayType.isDate(displayType)) {
				//cs.setDataFormat(m_dataFormat.getFormat("DD.MM.YYYY"));
				if (formatPattern != null) {
					cs.setDataFormat(dataFormat.getFormat(formatPattern));
				} else {
					SimpleDateFormat sdf = DisplayType.getDateFormat(DisplayType.Date, getLanguage());
					cs.setDataFormat(dataFormat.getFormat(sdf.toPattern()));
				}
			}
			else if (DisplayType.isNumeric(displayType)) {
				if (formatPattern != null) {
					cs.setDataFormat(dataFormat.getFormat(formatPattern));
				} else {
					DecimalFormat df = DisplayType.getNumberFormat(displayType, getLanguage());
					String format = getFormatString(df, isHighlightNegativeNumbers);
					cs.setDataFormat(dataFormat.getFormat(format));
				}
			}
			styles.put(key, cs);
		}
		return cs;
	}
	
	/**
	 * Get Excel number format string by given {@link NumberFormat}
	 * @param df number format
	 * @param isHighlightNegativeNumbers highlight negative numbers using RED color
	 * @return number excel format string
	 */
	private String getFormatString(NumberFormat df, boolean isHighlightNegativeNumbers) {
		StringBuffer format = new StringBuffer();
		int integerDigitsMin = df.getMinimumIntegerDigits();
		int integerDigitsMax = df.getMaximumIntegerDigits();
		for (int i = 0; i < integerDigitsMax; i++) {
			if (i < integerDigitsMin)
				format.insert(0, "0");
			else
				format.insert(0, "#");
			if (i == 2) {
				format.insert(0, ",");
			}
		}
		int fractionDigitsMin = df.getMinimumFractionDigits();
		int fractionDigitsMax = df.getMaximumFractionDigits();
		for (int i = 0; i < fractionDigitsMax; i++) {
			if (i == 0)
				format.append(".");
			if (i < fractionDigitsMin)
				format.append("0");
			else
				format.append("#");
		}
		if (isHighlightNegativeNumbers) {
			String f = format.toString();
			format = new StringBuffer(f).append(";[RED]-").append(f);
		}
		//
		return format.toString();

	}
	
	private Font getHeaderFont() {
		Font font = null;
		if (fontHeader == null) {
			fontHeader = workBook.createFont();
			fontHeader.setBold(true);
		}
		font = fontHeader;
		return font;
	}
	
	private Font getCellFont() {
		Font font = null;
		if (fontDefault == null) {
			fontDefault = workBook.createFont();
		}
		font = fontDefault;
		return font;
	}
	
	private Font getCellSummaryFont() {
		Font font = workBook.createFont();
		font.setBold(true);
		font.setItalic(true);
		return font;
	}
	
	private String writeFile() {
		try {
			File file = File.createTempFile("Report_Engine_", ".xlsx");
			FileOutputStream out = new FileOutputStream(file .getAbsolutePath());
		    workBook.write(out);
		    out.close();
		    workBook.dispose();
		    return file.getName();
		} catch (Exception e) {
			throw new AdempiereException(e);
		}
	}
}
