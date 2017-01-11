/*
 * Copyright 2015 TieFaces.
 * Licensed under MIT
 */
package org.tiefaces.components.websheet.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.NameIdentifier;
import org.apache.poi.ss.formula.SheetIdentifier;
import org.apache.poi.ss.formula.ptg.Area3DPtg;
import org.apache.poi.ss.formula.ptg.Area3DPxg;
import org.apache.poi.ss.formula.ptg.AreaPtg;
import org.apache.poi.ss.formula.ptg.AreaPtgBase;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.FuncVarPtg;
import org.apache.poi.ss.formula.ptg.OperationPtg;
import org.apache.poi.ss.formula.ptg.ParenthesisPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.Ref3DPtg;
import org.apache.poi.ss.formula.ptg.Ref3DPxg;
import org.apache.poi.ss.formula.ptg.RefPtg;
import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.formula.ptg.AddPtg;
import org.apache.poi.ss.formula.ptg.ValueOperatorPtg;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.tiefaces.components.websheet.configuration.RowsMapping;
import org.tiefaces.components.websheet.configuration.ShiftFormulaRef;

/**
 * The Class ShiftFormula.
 */
public final class ShiftFormulaUtility {

	/**
	 * hide constructor.
	 */
	private ShiftFormulaUtility() {
		// not called
	}

	/** The row wrapping mask. */
	// only support xlsx
	private static int _rowWrappingMask = SpreadsheetVersion.EXCEL2007
			.getLastRowIndex();

	/**
	 * Convert shared formulas.
	 *
	 * @param ptgs
	 *            the ptgs
	 * @param shiftFormulaRef
	 *            the shift formula ref
	 * @return the ptg[]
	 */
	public static Ptg[] convertSharedFormulas(final Ptg[] ptgs,
			final ShiftFormulaRef shiftFormulaRef) {

		List<Ptg> newPtgList = new ArrayList<Ptg>();
		Object ptg = null;
		for (int k = 0; k < ptgs.length; ++k) {
			ptg = ptgs[k];
			newPtgList.addAll(Arrays.asList(convertPtg(ptgs, k,
					shiftFormulaRef, ptg)));
		}

		// String [] stockArr = stockList.toArray(new String[stockList.size()]);
		return newPtgList.toArray(new Ptg[newPtgList.size()]);
	}

	/**
	 * 3d (reference to other sheet) is not supported. only 2d (within the
	 * sheet) is supported.
	 * 
	 * @param ptg
	 *            input ptg object.
	 * @return -1 if not a supported ptg. Int row number.
	 */
	public static int getFirstSupportedRowNumFromPtg(final Object ptg) {

		int rCode = -1;

		if (ptg instanceof RefPtgBase) {
			if (ptg instanceof Ref3DPxg) {
				// not supported
			} else if (ptg instanceof Ref3DPtg) {
				// not supported
			} else {
				rCode = ((RefPtgBase) ptg).getRow();
			}
		} else if (ptg instanceof AreaPtgBase) {
			if (ptg instanceof Area3DPxg) {
				// not supported
			} else if (ptg instanceof Area3DPtg) {
				// not supported
			} else {
				rCode = ((AreaPtgBase) ptg).getFirstRow();
			}
		}

		return rCode;
	}

	/**
	 * Convert ptg.
	 *
	 * @param ptgs
	 *            the ptgs
	 * @param position
	 *            the position
	 * @param shiftFormulaRef
	 *            the shift formula ref
	 * @param ptg
	 *            the ptg
	 * @return the ptg[]
	 */
	private static Ptg[] convertPtg(final Ptg[] ptgs, final int position,
			final ShiftFormulaRef shiftFormulaRef, final Object ptg) {

		// SharedFormula sharedFormula.convertSharedFormulas(sharedFormulaPtg,
		// 0, 1);
		byte originalOperandClass = -1;

		if (!((Ptg) ptg).isBaseToken()) {
			originalOperandClass = ((Ptg) ptg).getPtgClass();
		}

		int currentRow;
		currentRow = getFirstSupportedRowNumFromPtg(ptg);
		if ((currentRow >= 0)
				&& shiftFormulaRef.getWatchList().contains(currentRow)) {
			List<Row> rowlist = getRowsList(currentRow,
					shiftFormulaRef.getCurrentRowsMappingList());
			if ((rowlist == null) || (rowlist.size() == 0)) {
				// no need change ptg
				return singlePtg(ptg, originalOperandClass, -1);
			} else {
				shiftFormulaRef.setFormulaChanged(1);
				// one to one or has no round brackets
				if ((rowlist.size() == 1)
						|| ((position + 1) >= ptgs.length)
						|| !(ptgs[position + 1] instanceof ParenthesisPtg)) {
					// change ptg one to one
					// return changed ptg
					return singlePtg(
							fixupRefRelativeRowOneToOne(ptg, rowlist.get(0)),
							originalOperandClass, -1);
				} else {
					shiftFormulaRef.setFormulaChanged(rowlist.size());
					return fixupRefRelativeRowOneToMany(ptg,
							originalOperandClass, rowlist, ptgs, position);
				}
			}

		} else {
			// no need change ptg
			if ((ptg instanceof AttrPtg)
					&& (shiftFormulaRef.getFormulaChanged() > 1)) {
				AttrPtg newPtg = (AttrPtg) ptg;
				if (newPtg.isSum()) {
					FuncVarPtg fptg = FuncVarPtg.create("sum",
							shiftFormulaRef.getFormulaChanged());
					return singlePtg(fptg, fptg.getPtgClass(),
							shiftFormulaRef.getFormulaChanged());
				}
			}
			return singlePtg(ptg, originalOperandClass,
					shiftFormulaRef.getFormulaChanged());
		}
	}

	/**
	 * Single ptg.
	 *
	 * @param ptg
	 *            the ptg
	 * @param originalOperandClass
	 *            the original operand class
	 * @param formulaChanged
	 *            the formula changed
	 * @return the ptg[]
	 */
	private static Ptg[] singlePtg(Object ptg,
			final byte originalOperandClass, final int formulaChanged) {
		Ptg[] newPtg = new Ptg[1];
		if (originalOperandClass != (-1)) {
			((Ptg) ptg).setClass(originalOperandClass);
		}
		if (ptg instanceof FuncVarPtg) {
			FuncVarPtg fptg = (FuncVarPtg) ptg;
			if ((formulaChanged > 0)
					&& (fptg.getNumberOfOperands() != formulaChanged)) {
				ptg = FuncVarPtg.create(((FuncVarPtg) ptg).getName(),
						formulaChanged);
			}
		}
		newPtg[0] = (Ptg) ptg;
		return newPtg;
	}

	/**
	 * Gets the rows list.
	 *
	 * @param currentRow
	 *            the current row
	 * @param currentRowsMappingList
	 *            the current rows mapping list
	 * @return the rows list
	 */
	private static List<Row> getRowsList(final int currentRow,
			final List<RowsMapping> currentRowsMappingList) {
		List<Row> first = null;
		List<Row> all = null;
		int count = 0;
		for (RowsMapping rowsmapping : currentRowsMappingList) {
			List<Row> current = rowsmapping.get(currentRow);
			if (current != null) {
				count++;
				if (count == 1) {
					first = current;
				} else {
					if (all == null) {
						all = new ArrayList<Row>();
						all.addAll(first);
					}
					for (Row row : current) {
						if (!all.contains(row)) {
							all.addAll(current);
						}
					}
				}
			}
		}
		if (count <= 1) {
			return first;
		} else {
			return all;
		}
	}

	/**
	 * Fixup ref relative row one to one.
	 *
	 * @param ptg
	 *            the ptg
	 * @param newRow
	 *            the new row
	 * @return the object
	 */
	protected static Object fixupRefRelativeRowOneToOne(Object ptg,
			final Row newRow) {
		if (ptg instanceof RefPtgBase) {
			if (ptg instanceof Ref3DPxg) {
				Ref3DPxg ref3dPxg = (Ref3DPxg) ptg;
				Ref3DPxg new3dpxg = new Ref3DPxg(
						ref3dPxg.getExternalWorkbookNumber(),
						new SheetIdentifier(null, new NameIdentifier(
								ref3dPxg.getSheetName(), false)),
						new CellReference(newRow.getRowNum(), ref3dPxg
								.getColumn()));
				new3dpxg.setClass(ref3dPxg.getPtgClass());
				new3dpxg.setColRelative(ref3dPxg.isColRelative());
				new3dpxg.setRowRelative(ref3dPxg.isRowRelative());
				new3dpxg.setLastSheetName(ref3dPxg.getLastSheetName());
				return new3dpxg;
			} else {
				RefPtgBase refPtgBase = (RefPtgBase) ptg;
				RefPtgBase newRefPtg = new RefPtg(newRow.getRowNum(),
						refPtgBase.getColumn(), refPtgBase.isRowRelative(),
						refPtgBase.isColRelative());
				return newRefPtg;
			}
		} else {
			if (ptg instanceof Area3DPxg) {
				Area3DPxg area3dPxg = (Area3DPxg) ptg;
				Area3DPxg new3dpxg = new Area3DPxg(
						area3dPxg.getExternalWorkbookNumber(),
						new SheetIdentifier(null, new NameIdentifier(
								area3dPxg.getSheetName(), false)),
						area3dPxg.format2DRefAsString());
				new3dpxg.setClass(area3dPxg.getPtgClass());
				new3dpxg.setFirstColRelative(area3dPxg.isFirstColRelative());
				new3dpxg.setLastColRelative(area3dPxg.isLastColRelative());
				int shiftRow = newRow.getRowNum() - area3dPxg.getFirstRow();
				new3dpxg.setFirstRow(area3dPxg.getFirstRow() + shiftRow);
				new3dpxg.setLastRow(area3dPxg.getLastRow() + shiftRow);
				new3dpxg.setFirstRowRelative(area3dPxg.isFirstRowRelative());
				new3dpxg.setLastRowRelative(area3dPxg.isLastRowRelative());
				new3dpxg.setLastSheetName(area3dPxg.getLastSheetName());
				return new3dpxg;
			} else {
				AreaPtgBase areaPtgBase = (AreaPtgBase) ptg;
				int shiftRow = newRow.getRowNum()
						- areaPtgBase.getFirstRow();
				AreaPtgBase newAreaPtg = new AreaPtg(
						areaPtgBase.getFirstRow() + shiftRow,
						areaPtgBase.getLastRow() + shiftRow,
						areaPtgBase.getFirstColumn(),
						areaPtgBase.getLastColumn(),
						areaPtgBase.isFirstRowRelative(),
						areaPtgBase.isLastRowRelative(),
						areaPtgBase.isFirstColRelative(),
						areaPtgBase.isLastColRelative());
				return newAreaPtg;
			}
		}

	}

	/**
	 * Change formula ptg by replace one ref with multiple ref. We require user
	 * follow the rule to define dynamic formula. e.g. If cell reference in
	 * formula maybe become multiple cells, then should use round brackets
	 * around it.
	 * 
	 * Case 1: = (A1) + A2 + A3 Case 2: = SUM((A1)) Case 3: = SUM((A1:A2))
	 *
	 * @param ptg
	 *            the ptg
	 * @param originalOperandClass
	 *            the original operand class
	 * @param rowList
	 *            the row list
	 * @param ptgs
	 *            the ptgs
	 * @param position
	 *            the position
	 * @return the ptg[]
	 */
	protected static Ptg[] fixupRefRelativeRowOneToMany(final Object ptg,
			final byte originalOperandClass, final List<Row> rowList,
			final Ptg[] ptgs, final int position) {
		int size = rowList.size();
		Ptg[] newPtg = null;
		// if followedby valueoperator, then change to multiple ptg plus Add
		// e.g. (A1) --> (A1+A2)
		if (isFollowedByValueOperator(ptgs, position)) {
			if (ptg instanceof RefPtgBase) {
				newPtg = new Ptg[size + 1];
				buildDynamicRowForRefPtgBase(ptg, originalOperandClass,
						rowList, newPtg, false);
				newPtg[rowList.size()] = AddPtg.instance;
			}
		} else {
			// otherwise change to mutiple ptg plus parenth
			// e.g. SUM((A1)) --> SUM((A1),(A2))
			// SUM((A1:B1)) --> SUM((A1:B1),(A2:B2))
			newPtg = new Ptg[(size * 2) - 1];
			if (ptg instanceof RefPtgBase) {
				buildDynamicRowForRefPtgBase(ptg, originalOperandClass,
						rowList, newPtg, true);
			} else {
				buildDynamicRowForAreaPtgBase(ptg, originalOperandClass,
						rowList, newPtg);
			}
		}
		return newPtg;
	}

	/**
	 * Builds the dynamic row for ref ptg base.
	 *
	 * @param ptg
	 *            the ptg
	 * @param originalOperandClass
	 *            the original operand class
	 * @param rowList
	 *            the row list
	 * @param newPtg
	 *            the new ptg
	 * @param includeParenthesis
	 *            the include parenthesis
	 */
	private static void buildDynamicRowForRefPtgBase(final Object ptg,
			final byte originalOperandClass, final List<Row> rowList,
			final Ptg[] newPtg, final boolean includeParenthesis) {
		RefPtgBase refPtg = (RefPtgBase) ptg;
		int unitSize = 1;
		if (includeParenthesis) {
			unitSize = 2;
		}
		for (int i = 0; i < rowList.size(); i++) {
			Row row = rowList.get(i);
			if (refPtg instanceof Ref3DPxg) {
				Ref3DPxg ref3dPxg = (Ref3DPxg) refPtg;
				Ref3DPxg new3dpxg = new Ref3DPxg(
						ref3dPxg.getExternalWorkbookNumber(),
						new SheetIdentifier(null, new NameIdentifier(
								ref3dPxg.getSheetName(), false)),
						new CellReference(row.getRowNum(), ref3dPxg
								.getColumn()));
				new3dpxg.setClass(originalOperandClass);
				new3dpxg.setColRelative(ref3dPxg.isColRelative());
				new3dpxg.setRowRelative(ref3dPxg.isRowRelative());
				new3dpxg.setLastSheetName(ref3dPxg.getLastSheetName());
				newPtg[i * unitSize] = new3dpxg;
			} else {
				RefPtgBase refPtgBase = (RefPtgBase) refPtg;
				newPtg[i * unitSize] = new RefPtg(row.getRowNum(),
						refPtgBase.getColumn(), refPtgBase.isRowRelative(),
						refPtgBase.isColRelative());
			}
			if ((unitSize == 2) && (i < (rowList.size() - 1))) {
				newPtg[i * unitSize + 1] = ParenthesisPtg.instance;
			}
		}
	}

	/**
	 * Builds the dynamic row for area ptg base.
	 *
	 * @param ptg
	 *            the ptg
	 * @param originalOperandClass
	 *            the original operand class
	 * @param rowList
	 *            the row list
	 * @param newPtg
	 *            the new ptg
	 */
	private static void buildDynamicRowForAreaPtgBase(final Object ptg,
			final byte originalOperandClass, final List<Row> rowList,
			final Ptg[] newPtg) {
		AreaPtgBase areaPtg = (AreaPtgBase) ptg;
		int originFirstRow = areaPtg.getFirstRow();
		int originLastRow = areaPtg.getLastRow();
		int unitSize = 2;
		for (int i = 0; i < rowList.size(); i++) {
			Row row = rowList.get(i);
			int shiftRow = row.getRowNum() - originFirstRow;
			if (ptg instanceof Area3DPxg) {
				Area3DPxg area3dPxg = (Area3DPxg) ptg;
				Area3DPxg new3dpxg = new Area3DPxg(
						area3dPxg.getExternalWorkbookNumber(),
						new SheetIdentifier(null, new NameIdentifier(
								area3dPxg.getSheetName(), false)),
						area3dPxg.format2DRefAsString());
				new3dpxg.setClass(originalOperandClass);
				new3dpxg.setFirstColRelative(area3dPxg.isFirstColRelative());
				new3dpxg.setLastColRelative(area3dPxg.isLastColRelative());
				new3dpxg.setFirstRow(originFirstRow + shiftRow);
				new3dpxg.setLastRow(originLastRow + shiftRow);
				new3dpxg.setFirstRowRelative(area3dPxg.isFirstRowRelative());
				new3dpxg.setLastRowRelative(area3dPxg.isLastRowRelative());
				new3dpxg.setLastSheetName(area3dPxg.getLastSheetName());
				newPtg[i * unitSize] = new3dpxg;
			} else {
				AreaPtgBase areaPtgBase = (AreaPtgBase) ptg;
				newPtg[i * unitSize] = new AreaPtg(originFirstRow
						+ shiftRow, originLastRow + shiftRow,
						areaPtgBase.getFirstColumn(),
						areaPtgBase.getLastColumn(),
						areaPtgBase.isFirstRowRelative(),
						areaPtgBase.isLastRowRelative(),
						areaPtgBase.isFirstColRelative(),
						areaPtgBase.isLastColRelative());

			}
			if (i < (rowList.size() - 1)) {
				newPtg[i * unitSize + 1] = ParenthesisPtg.instance;
			}
		}
	}

	/**
	 * check is current ptg followed by valueOperationPtg. valueOperationPtg
	 * include: Add, SubStract, Multiply, Divide etc.
	 *
	 * @param ptgs
	 *            the ptgs
	 * @param position
	 *            the position
	 * @return true, if is followed by value operator
	 */

	private static boolean isFollowedByValueOperator(final Ptg[] ptgs,
			final int position) {

		for (int i = position; i < ptgs.length; i++) {
			Object ptg = ptgs[position];
			if (ptg instanceof OperationPtg) {
				return ptg instanceof ValueOperatorPtg;
			} else if (ptg instanceof AttrPtg) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Fixup relative row.
	 *
	 * @param shift
	 *            the shift
	 * @param row
	 *            the row
	 * @param relative
	 *            the relative
	 * @return the int
	 */

	protected static int fixupRelativeRow(final int shift, final int row,
			final boolean relative) {
		if (relative) {
			return row + shift & _rowWrappingMask;
		} else {
			return row;
		}

	}

}
