/*
 * Copyright 2017 TieFaces.
 * Licensed under MIT
 */

package org.tiefaces.components.websheet.utility;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetDimension;
import org.tiefaces.common.TieConstants;
import org.tiefaces.components.websheet.configuration.ConfigRange;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class TieWebSheetUtility.
 */
public final class WebSheetUtility {

	/** logger. */
	private static final Logger LOG = Logger
			.getLogger(WebSheetUtility.class.getName());

	// Each cell conatins a fixed number of co-ordinate points; this number
	// does not vary with row height or column width or with font. These two
	/** The Constant TOTAL_COLUMN_COORDINATE_POSITIONS. */
	// constants are defined below.
	public static final int TOTAL_COLUMN_COORDINATE_POSITIONS = 1023; // MB

	/** The Constant TOTAL_ROW_COORDINATE_POSITIONS. */
	public static final int TOTAL_ROW_COORDINATE_POSITIONS = 255; // MB
	// The resoultion of an image can be expressed as a specific number
	// of pixels per inch. Displays and printers differ but 96 pixels per
	/** The Constant PIXELS_PER_INCH. */
	// inch is an acceptable standard to beging with.
	public static final int PIXELS_PER_INCH = 96; // MB
	/** The Constant MILLIMETERS_PER_INCH. */
	public static final double MILLIMETERS_PER_INCH = 25.4;
	/** The Constant POINTS_PER_INCH. */
	public static final double POINTS_PER_INCH = 72D;
	// Cnstants that defines how many pixels and points there are in a
	/** The Constant PIXELS_PER_MILLIMETRES. */
	// millimetre. These values are required for the conversion algorithm.
	public static final double PIXELS_PER_MILLIMETRES = 3.78; // MB

	/** The Constant PICTURE_HEIGHT_ADJUST. */
	// These values are required for the conversion algorithm.
	public static final double PICTURE_HEIGHT_ADJUST = 3.03125; // MB

	/** The Constant POINTS_PER_MILLIMETRE. */
	public static final double POINTS_PER_MILLIMETRE = 2.83; // MB
	// The column width returned by HSSF and the width of a picture when
	// positioned to exactly cover one cell are different by almost exactly
	// 2mm - give or take rounding errors. This constant allows that
	// additional amount to be accounted for when calculating how many
	/** The Constant CELL_BORDER_WIDTH_MILLIMETRES. */
	// celles the image ought to overlie.
	public static final double CELL_BORDER_WIDTH_MILLIMETRES = 2.0D; // MB

	/** The Constant EXCEL_COLUMN_WIDTH_FACTOR. */
	public static final short EXCEL_COLUMN_WIDTH_FACTOR = 256;

	/** The Constant UNIT_OFFSET_LENGTH. */
	public static final int UNIT_OFFSET_LENGTH = 7;

	/** The Constant UNIT_OFFSET_MAP. */
	static final int[] UNIT_OFFSET_MAP = new int[] { 0, 36, 73, 109, 146,
			182, 219 };

	/** The Constant EXCEL_ROW_HEIGHT_FACTOR. */
	public static final short EXCEL_ROW_HEIGHT_FACTOR = 20;

	/** The Constant EMU_PER_MM. */
	public static final int EMU_PER_MM = 36000;

	/** The Constant EMU_PER_POINTS. */
	public static final int EMU_PER_POINTS = 12700;

	/** The Constant DATE_REGEX_YEAR_COMM_1. */
	private static final String DATE_REGEX_YEAR_COMM_1 = "([-/.\\\\]{1})";

	/** The Constant DATE_REGEX_YEAR_COMM_2. */
	private static final String DATE_REGEX_YEAR_COMM_2 = "[0?[1-9]|[1-9]|1[012]]{1,2}";

	/** The Constant DATE_REGEX_YEAR_COMM_3. */
	private static final String DATE_REGEX_YEAR_COMM_3 = "([0?[1-9]|[1-9]|1[0-9]|2[0-9]|3[01]]{1,2})";

	/** The Constant DATE_REGEX_4_DIGIT_YEAR. */
	private static final String DATE_REGEX_4_DIGIT_YEAR = "("
			+ "(19|20)[0-9]{2}" + DATE_REGEX_YEAR_COMM_1
			+ DATE_REGEX_YEAR_COMM_2 + "\\3" + DATE_REGEX_YEAR_COMM_3 + ")"
			+ "|" + "(" + DATE_REGEX_YEAR_COMM_2 + DATE_REGEX_YEAR_COMM_1
			+ DATE_REGEX_YEAR_COMM_3 + "\\6" + "(19|20)[0-9]{2}" + ")";

	/** The Constant DATE_REGEX_2_DIGIT_YEAR. */
	private static final String DATE_REGEX_2_DIGIT_YEAR = "(" + "[0-9]{2}"
			+ DATE_REGEX_YEAR_COMM_1 + DATE_REGEX_YEAR_COMM_2 + "\\3"
			+ DATE_REGEX_YEAR_COMM_3 + ")" + "|" + "("
			+ DATE_REGEX_YEAR_COMM_2 + DATE_REGEX_YEAR_COMM_1
			+ DATE_REGEX_YEAR_COMM_3 + "\\6" + "[0-9]{2}" + ")";

	/**
	 * PIXEL_HEIGHT_ASPC_ADJUST.
	 */
	private static final double PIXEL_HEIGHT_ASPC_ADJUST = 14;

	/**
	 * hide constructor.
	 */
	private WebSheetUtility() {
		// not called
	}

	/**
	 * Gets the excel column name.
	 *
	 * @param pnumber
	 *            the number
	 * @return the string
	 */
	public static String getExcelColumnName(final int pnumber) {
		StringBuilder converted = new StringBuilder();
		// Repeatedly divide the number by 26 and convert the
		// remainder into the appropriate letter.
		int number = pnumber;
		while (number >= 0) {
			int remainder = number % TieConstants.EXCEL_LETTER_NUMBERS;
			converted.insert(0, (char) (remainder + 'A'));
			number = (number / TieConstants.EXCEL_LETTER_NUMBERS) - 1;
		}

		return converted.toString();
	}

	/**
	 * return full name for cell with sheet name and $ format e.g. Sheet1$A$1
	 * 
	 * @param sheet1
	 *            sheet
	 * @param cell
	 *            cell
	 * @return String full cell reference name
	 */

	public static String getFullCellRefName(final Sheet sheet1,
			final Cell cell) {
		if ((sheet1 != null) && (cell != null)) {
			return sheet1.getSheetName() + "!$"
					+ getExcelColumnName(cell.getColumnIndex()) + "$"
					+ (cell.getRowIndex() + 1);
		}
		return null;
	}

	/**
	 * return full name for cell with sheet name and $ format e.g. Sheet1$A$1
	 *
	 * @param sheetName
	 *            the sheet name
	 * @param rowIndex
	 *            the row index
	 * @param colIndex
	 *            the col index
	 * @return String full cell reference name
	 */

	public static String getFullCellRefName(final String sheetName,
			final int rowIndex, final int colIndex) {
		if (sheetName != null) {
			return sheetName + "!$" + getExcelColumnName(colIndex) + "$"
					+ (rowIndex + 1);
		}
		return null;
	}

	/**
	 * return sheet name from cell full name e.g. return Sheet1 from Sheet1$A$1
	 *
	 * @param fullName
	 *            the full name
	 * @return String Sheet Name
	 */

	public static String getSheetNameFromFullCellRefName(
			final String fullName) {
		if ((fullName != null) && (fullName.contains("!"))) {
			return fullName.substring(0, fullName.indexOf('!'));
		}
		return null;
	}

	/**
	 * remove sheet name from cell full name e.g. return $A$1 from Sheet1$A$1
	 *
	 * @param fullName
	 *            the full name
	 * @return remove Sheet Name from full name
	 */

	public static String removeSheetNameFromFullCellRefName(
			final String fullName) {
		if ((fullName != null) && (fullName.contains("!"))) {
			return fullName.substring(fullName.indexOf('!') + 1);
		}
		return fullName;
	}

	/**
	 * Convert col to int.
	 *
	 * @param col
	 *            the col
	 * @return the int
	 */
	public static int convertColToInt(final String col) {
		String name = col.toUpperCase();
		int number = 0;
		int pow = 1;
		for (int i = name.length() - 1; i >= 0; i--) {
			number += (name.charAt(i) - 'A' + 1) * pow;
			pow *= TieConstants.EXCEL_LETTER_NUMBERS;
		}

		return number - 1;
	}

	/**
	 * Gets the cell by reference.
	 *
	 * @param cellRef
	 *            the cell ref
	 * @param sheet
	 *            the sheet
	 * @return the cell by reference
	 */
	public static Cell getCellByReference(final String cellRef,
			final Sheet sheet) {

		Cell c = null;
		try {
			CellReference ref = new CellReference(cellRef);
			Row r = sheet.getRow(ref.getRow());
			if (r != null) {
				c = r.getCell(ref.getCol(),
						MissingCellPolicy.CREATE_NULL_AS_BLANK);
			}
		} catch (Exception ex) {
			// use log.debug because mostly it's expected
			LOG.log(Level.SEVERE,
					"WebForm WebFormHelper getCellByReference cellRef = "
							+ cellRef + "; error = "
							+ ex.getLocalizedMessage(),
					ex);
		}
		return c;
	}

	/**
	 * pixel units to excel width units(units of 1/256th of a character width).
	 *
	 * @param pxs
	 *            the pxs
	 * @return the short
	 */
	public static short pixel2WidthUnits(final int pxs) {
		short widthUnits = (short) (EXCEL_COLUMN_WIDTH_FACTOR
				* (pxs / UNIT_OFFSET_LENGTH));
		widthUnits += UNIT_OFFSET_MAP[pxs % UNIT_OFFSET_LENGTH];
		return widthUnits;
	}

	/**
	 * excel width units(units of 1/256th of a character width) to pixel units.
	 *
	 * @param widthUnits
	 *            the width units
	 * @return the int
	 */
	public static int widthUnits2Pixel(final int widthUnits) {
		int pixels = (widthUnits / EXCEL_COLUMN_WIDTH_FACTOR)
				* UNIT_OFFSET_LENGTH;
		int offsetWidthUnits = widthUnits % EXCEL_COLUMN_WIDTH_FACTOR;
		pixels += Math.round(offsetWidthUnits
				/ ((float) EXCEL_COLUMN_WIDTH_FACTOR / UNIT_OFFSET_LENGTH));
		return pixels;
	}

	/**
	 * Height units 2 pixel.
	 *
	 * @param heightUnits
	 *            the height units
	 * @return the int
	 */
	public static int heightUnits2Pixel(final short heightUnits) {
		int pixels = heightUnits / EXCEL_ROW_HEIGHT_FACTOR;
		int offsetHeightUnits = heightUnits % EXCEL_ROW_HEIGHT_FACTOR;
		pixels += Math.round((float) offsetHeightUnits
				/ ((float) EXCEL_COLUMN_WIDTH_FACTOR / UNIT_OFFSET_LENGTH
						/ 2));
		pixels += (Math.floor(pixels / PIXEL_HEIGHT_ASPC_ADJUST) + 1) * 4;

		return pixels;
	}

	/**
	 * Convert Excels width units into millimetres.
	 *
	 * @param widthUnits
	 *            The width of the column or the height of the row in Excels
	 *            units.
	 * @return A primitive double that contains the columns width or rows height
	 *         in millimetres.
	 */
	public static double widthUnits2Millimetres(final short widthUnits) {
		return widthUnits2Pixel(widthUnits) / PIXELS_PER_MILLIMETRES;
	}

	/**
	 * Convert into millimetres Excels width units..
	 *
	 * @param millimetres
	 *            A primitive double that contains the columns width or rows
	 *            height in millimetres.
	 * @return A primitive int that contains the columns width or rows height in
	 *         Excels units.
	 */
	public static int millimetres2WidthUnits(final double millimetres) {
		return pixel2WidthUnits(
				(int) (millimetres * PIXELS_PER_MILLIMETRES));
	}

	/**
	 * Points to pixels.
	 *
	 * @param points
	 *            the points
	 * @return the int
	 */
	public static int pointsToPixels(final double points) {
		return (int) Math.round(points / POINTS_PER_INCH * PIXELS_PER_INCH);
	}

	/**
	 * Points to millimeters.
	 *
	 * @param points
	 *            the points
	 * @return the double
	 */
	public static double pointsToMillimeters(final double points) {
		return points / POINTS_PER_INCH * MILLIMETERS_PER_INCH;
	}

	/**
	 * Checks if is date.
	 *
	 * @param s
	 *            the s
	 * @return true, if is date
	 */
	public static boolean isDate(final String s) {
		Pattern pattern = Pattern.compile(DATE_REGEX_4_DIGIT_YEAR);
		String[] terms = s.split(" ");
		Matcher matcher;
		for (String term : terms) {
			matcher = pattern.matcher(term);
			if (matcher.matches()) {
				return true;
			}
		}
		pattern = Pattern.compile(DATE_REGEX_2_DIGIT_YEAR);
		terms = s.split(" ");
		for (String term : terms) {
			matcher = pattern.matcher(term);
			if (matcher.matches()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses the date.
	 *
	 * @param entry
	 *            the entry
	 * @return the string
	 */
	public static String parseDate(final String entry) {
		Pattern pattern = Pattern.compile(DATE_REGEX_4_DIGIT_YEAR);
		String[] terms = entry.split(" ");
		Matcher matcher;
		for (String term : terms) {
			matcher = pattern.matcher(term);
			if (matcher.matches()) {
				return matcher.group();
			}
		}
		pattern = Pattern.compile(DATE_REGEX_2_DIGIT_YEAR);
		terms = entry.split(" ");
		for (String term : terms) {
			matcher = pattern.matcher(term);
			if (matcher.matches()) {
				return matcher.group();
			}
		}
		return "";
	}

	/**
	 * Checks if is numeric.
	 *
	 * @param str
	 *            the str
	 * @return true, if is numeric
	 */
	public static boolean isNumeric(final String str) {

		String s = str;
		if (s.startsWith("-")) {
			s = s.substring(1);
		}
		char c;
		int i;
		int sLen = s.length();
		ShouldContinueParameter sPara = new ShouldContinueParameter(false,
				false, 0);

		for (i = 0; i < sLen; i++) {
			c = s.charAt(i);
			if (c < '0' || c > '9') {
				if (!shouldContinue(c, sPara)) {
					return false;
				}
			} else {
				if (sPara.isCommaHit()) {
					sPara.setSinceLastComma(sPara.getSinceLastComma() + 1);
				}
			}
		}
		return true;
	}

	/**
	 * The Class ShouldContinueParameter.
	 */
	private static class ShouldContinueParameter {

		/** The decimal hit. */
		private Boolean decimalHit;

		/** The comma hit. */
		private Boolean commaHit;

		/** The since last comma. */
		private Integer sinceLastComma;

		/**
		 * Instantiates a new should continue parameter.
		 *
		 * @param pdecimalHit
		 *            the decimal hit
		 * @param pcommaHit
		 *            the comma hit
		 * @param psinceLastComma
		 *            the since last comma
		 */
		ShouldContinueParameter(final Boolean pdecimalHit,
				final Boolean pcommaHit, final Integer psinceLastComma) {
			this.decimalHit = pdecimalHit;
			this.commaHit = pcommaHit;
			this.sinceLastComma = psinceLastComma;
		}

		/**
		 * Checks if is decimal hit.
		 *
		 * @return the boolean
		 */
		public Boolean isDecimalHit() {
			return decimalHit;
		}

		/**
		 * Sets the decimal hit.
		 *
		 * @param pdecimalHit
		 *            the new decimal hit
		 */
		public void setDecimalHit(final Boolean pdecimalHit) {
			this.decimalHit = pdecimalHit;
		}

		/**
		 * Checks if is comma hit.
		 *
		 * @return the boolean
		 */
		public Boolean isCommaHit() {
			return commaHit;
		}

		/**
		 * Sets the comma hit.
		 *
		 * @param pcommaHit
		 *            the new comma hit
		 */
		public void setCommaHit(final Boolean pcommaHit) {
			this.commaHit = pcommaHit;
		}

		/**
		 * Gets the since last comma.
		 *
		 * @return the since last comma
		 */
		public Integer getSinceLastComma() {
			return sinceLastComma;
		}

		/**
		 * Sets the since last comma.
		 *
		 * @param psinceLastComma
		 *            the new since last comma
		 */
		public void setSinceLastComma(final Integer psinceLastComma) {
			this.sinceLastComma = psinceLastComma;
		}
	}

	/**
	 * Should continue.
	 *
	 * @param c
	 *            the c
	 * @param para
	 *            the para
	 * @return true, if successful
	 */
	private static boolean shouldContinue(final char c,
			final ShouldContinueParameter para) {
		if (c == '.' && !para.isDecimalHit()) {
			para.setDecimalHit(true);
			if (para.isCommaHit() && para.getSinceLastComma() != 3) {
				return false;
			}
			return true;
		} else if (c == ',' && !para.isDecimalHit()) {
			if (para.isCommaHit()) {
				if (para.getSinceLastComma() != 3) {
					return false;
				}
				para.setSinceLastComma(0);
			}
			para.setCommaHit(true);
			return true;
		}
		return false;
	}

	/**
	 * Sets the object property.
	 *
	 * @param obj
	 *            the obj
	 * @param propertyName
	 *            the property name
	 * @param propertyValue
	 *            the property value
	 * @param ignoreNonExisting
	 *            the ignore non existing
	 */
	public static void setObjectProperty(final Object obj,
			final String propertyName, final String propertyValue,
			final boolean ignoreNonExisting) {
		try {
			Method method = obj.getClass().getMethod(
					"set" + Character.toUpperCase(propertyName.charAt(0))
							+ propertyName.substring(1),
					new Class[] { String.class });
			method.invoke(obj, propertyValue);
		} catch (Exception e) {
			String msg = "failed to set property '" + propertyName
					+ "' to value '" + propertyValue + "' for object "
					+ obj;
			if (ignoreNonExisting) {
				LOG.info(msg);
			} else {
				LOG.warning(msg);
				throw new IllegalArgumentException(e);
			}
		}
	}

	/**
	 * Cell compare to.
	 *
	 * @param thisCell
	 *            the this cell
	 * @param otherCell
	 *            the other cell
	 * @return the int
	 */
	public static int cellCompareTo(final Cell thisCell,
			final Cell otherCell) {
		int r = thisCell.getRowIndex() - otherCell.getRowIndex();
		if (r != 0) {
			return r;
		}

		r = thisCell.getColumnIndex() - otherCell.getColumnIndex();
		if (r != 0) {
			return r;
		}

		return 0;
	}

	/**
	 * Inside range.
	 *
	 * @param child
	 *            the child
	 * @param parent
	 *            the parent
	 * @return true, if successful
	 */
	public static boolean insideRange(final ConfigRange child,
			final ConfigRange parent) {

		return ((cellCompareTo(child.getFirstRowRef(),
				parent.getFirstRowRef()) >= 0)
				&& (cellCompareTo(child.getLastRowPlusRef(),
						parent.getLastRowPlusRef()) <= 0));
	}

	/**
	 * return the last column of the sheet.
	 * 
	 * @param sheet
	 *            sheet.
	 * @return last column number (A column will return 0).
	 */
	public static int getSheetRightCol(final Sheet sheet) {

		try {
			if (sheet instanceof XSSFSheet) {
				XSSFSheet xsheet = (XSSFSheet) sheet;
				int rightCol = getSheetRightColFromDimension(xsheet);
				if (rightCol > TieConstants.MAX_COLUMNS_IN_SHEET) {
					clearHiddenColumns(sheet);
					rightCol = getSheetRightColFromDimension(xsheet);
				}
				return rightCol;

			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "error in getSheetRightCol : "
					+ e.getLocalizedMessage(), e);
		}
		return -1;
	}

	/**
	 * Clear hidden columns.
	 *
	 * @param sheet
	 *            the sheet
	 */
	public static void clearHiddenColumns(final Sheet sheet) {

		for (Row row : sheet) {
			if (row.getLastCellNum() > TieConstants.MAX_COLUMNS_IN_SHEET) {
				deleteHiddenColumnsInRow(row);
			}
		}

	}

	/**
	 * Delete hidden columns in row.
	 *
	 * @param row
	 *            the row
	 */
	private static void deleteHiddenColumnsInRow(final Row row) {
		deleteCellFromRow(row,
				TieConstants.HIDDEN_SAVE_OBJECTS_COLUMN);
		deleteCellFromRow(row,
				TieConstants.HIDDEN_ORIGIN_ROW_NUMBER_COLUMN);
		deleteCellFromRow(row,
				TieConstants.HIDDEN_FULL_NAME_COLUMN);
	}

	
	
	
	/**
	 * Delete cell from row.
	 *
	 * @param row
	 *            the row
	 * @param cellNum
	 *            the cell num
	 */
	private static void deleteCellFromRow(final Row row,
			final int cellNum) {
		Cell cell = row.getCell(cellNum);
		if (cell != null) {
			row.removeCell(cell);
		}
	}

	/**
	 * return the last column of the sheet.
	 *
	 * @param xsheet
	 *            the xsheet
	 * @return last column number (A column will return 0).
	 */
	private static int getSheetRightColFromDimension(
			final XSSFSheet xsheet) {
		CTSheetDimension dimension = xsheet.getCTWorksheet().getDimension();
		String sheetDimensions = dimension.getRef();
		if (sheetDimensions.indexOf(':') < 0) {
			return -1;
		} else {
			return CellRangeAddress.valueOf(sheetDimensions)
					.getLastColumn();
		}
	}
}
