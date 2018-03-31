/*******************************************************************************
 * Copyright (c) 2016 Jeremie Bresson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Jeremie Bresson - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.wikitext.asciidoc.internal.block;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.mylyn.wikitext.asciidoc.internal.util.LanguageSupport;
import org.eclipse.mylyn.wikitext.parser.DocumentBuilder.BlockType;
import org.eclipse.mylyn.wikitext.parser.TableAttributes;
import org.eclipse.mylyn.wikitext.parser.TableCellAttributes;
import org.eclipse.mylyn.wikitext.parser.TableRowAttributes;

/**
 * Text block containing a table
 */
public class TableBlock extends AsciiDocBlock {

	private TableFormat format;

	private String separator;

	private enum TableFormat {
		PREFIX_SEPARATED_VALUES, //PSV
		DELIMITER_SEPARATED_VALUES, //DSV
		COMMA_SEPARATED_VALUES //CSV
	}

	private int cellsCount = 0;

	private List<TableCellAttributes> colsAttribute;

	private boolean hasHeader = false;

	private boolean cellBlockIsOpen = false;

	public TableBlock() {
		super(Pattern.compile("^(\\||,|:)===\\s*")); //$NON-NLS-1$
	}

	@Override
	protected void processBlockStart() {

		if (startDelimiter.startsWith(",")) { //$NON-NLS-1$
			// ",===" is the shorthand notation for [format="csv", options="header"]
			format = TableFormat.COMMA_SEPARATED_VALUES;
			hasHeader = true;
		} else if (startDelimiter.startsWith(":")) { //$NON-NLS-1$
			// ":===" is the shorthand notation for [format="dsv", options="header"]
			format = TableFormat.DELIMITER_SEPARATED_VALUES;
			hasHeader = true;
		} else {
			// default table format is PSV with separator "|"
			format = TableFormat.PREFIX_SEPARATED_VALUES;
			separator = "|"; //$NON-NLS-1$
		}

		Map<String, String> lastProperties = getAsciiDocState().getLastProperties(Collections.emptyList());
		colsAttribute = LanguageSupport.computeColumnsAttributeList(lastProperties.get("cols")); //$NON-NLS-1$

		String formatProperty = lastProperties.get("format"); //$NON-NLS-1$
		if (formatProperty != null) {
			switch (formatProperty) {
			case "dsv": //$NON-NLS-1$
				format = TableFormat.DELIMITER_SEPARATED_VALUES;
				break;
			case "csv": //$NON-NLS-1$
				format = TableFormat.COMMA_SEPARATED_VALUES;
				break;
			}
		}

		String separator = lastProperties.get("separator"); //$NON-NLS-1$
		if (separator != null) {
			this.separator = separator;
		}

		String options = lastProperties.get("options"); //$NON-NLS-1$
		if (options != null) {
			hasHeader = options.contains("header"); //$NON-NLS-1$
		}

		TableAttributes tableAttributes = new TableAttributes();
		tableAttributes.setWidth(lastProperties.get("width")); //$NON-NLS-1$
		builder.beginBlock(BlockType.TABLE, tableAttributes);
	}

	private final boolean isColFormatKnown() {
		return !colsAttribute.isEmpty();
	}

	private final boolean isFirstRow() {
		return colsAttribute.isEmpty() || cellsCount < colsAttribute.size();
	}

	private final boolean isFirsCellOfTable() {
		return !cellBlockIsOpen;
	}

	@Override
	protected void processBlockContent(String line) {
		if (!line.trim().isEmpty()) {
			if (!isColFormatKnown() && !cellBlockIsOpen) {
				TableRowAttributes tableRowAttributes = new TableRowAttributes();
				builder.beginBlock(BlockType.TABLE_ROW, tableRowAttributes);
			}

			int offset = 0;
			boolean firstCellInLine = true;
			Matcher rowCellMatcher = createRowCellMatcher(line);

			while (offset <= line.length()) {
				boolean found = rowCellMatcher.find();
				int endOffset = found ? rowCellMatcher.start() : line.length();
				String cellContent = line.substring(offset, endOffset);
				if (offset == 0 && format == TableFormat.PREFIX_SEPARATED_VALUES) {
					if (!cellContent.isEmpty()) {
						if (isFirsCellOfTable()) {
							handleCellContent(cellContent, offset);
							firstCellInLine = false;
						} else {
							appendCellContent(cellContent, endOffset);
							firstCellInLine = false;
						}
					}
				} else {
					if (!isColFormatKnown() && firstCellInLine && !isFirsCellOfTable()) {
						colsAttribute = LanguageSupport.createDefaultColumnsAttributeList(cellsCount + 1);
					}
					// start of new cell
					handleCellContent(cellContent, offset);
					firstCellInLine = false;
				}
				offset = found ? rowCellMatcher.end() : line.length() + 1;
			}
		}
	}

	private Matcher createRowCellMatcher(String line) {
		if (format == TableFormat.COMMA_SEPARATED_VALUES) {
			String regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"; //$NON-NLS-1$
			return Pattern.compile(regex).matcher(line);
		}
		String delimiter = getCellSeparator();
		return Pattern.compile("(?<!\\\\)" + Pattern.quote(delimiter)).matcher(line); //$NON-NLS-1$
	}

	private String getCellSeparator() {
		switch (format) {
		case COMMA_SEPARATED_VALUES:
			return ","; //$NON-NLS-1$
		case DELIMITER_SEPARATED_VALUES:
			return ":"; //$NON-NLS-1$
		case PREFIX_SEPARATED_VALUES:
		default:
			return separator;
		}
	}

	private void handleCellContent(String fullCellContent, int positionInLine) {
		closeCellBlockIfNeeded();

		String cellContent = fullCellContent.trim();
		String blockContent;
		if (format == TableFormat.COMMA_SEPARATED_VALUES) {
			if (cellContent.startsWith("\"") && cellContent.endsWith("\"")) {
				blockContent = cellContent.substring(1, cellContent.length() - 1).replaceAll("\"\"", "\"");
			} else {
				blockContent = cellContent;
			}
		} else {
			String delimiter = getCellSeparator();
			blockContent = cellContent.replaceAll("\\\\" + Pattern.quote(delimiter), delimiter);//$NON-NLS-1$
		}

		if (isColFormatKnown() && cellsCount % colsAttribute.size() == 0) {
			TableRowAttributes tableRowAttributes = new TableRowAttributes();
			builder.beginBlock(BlockType.TABLE_ROW, tableRowAttributes);
		}

		TableCellAttributes attributes;
		if (colsAttribute.isEmpty()) {
			attributes = new TableCellAttributes();
		} else {
			attributes = colsAttribute.get(cellsCount % colsAttribute.size());
		}

		if (hasHeader && isFirstRow()) {
			builder.beginBlock(BlockType.TABLE_CELL_HEADER, attributes);
		} else {
			builder.beginBlock(BlockType.TABLE_CELL_NORMAL, attributes);
		}
		cellBlockIsOpen = true;

		int offset = fullCellContent.indexOf(cellContent);
		markupLanguage.emitMarkupLine(parser, state, offset + positionInLine, blockContent, 0);
	}

	private void appendCellContent(String cellContent, int positionInLine) {
		markupLanguage.emitMarkupLine(parser, state, positionInLine, " " + cellContent.trim(), 0);
	}

	@Override
	protected void processBlockEnd() {
		closeCellBlockIfNeeded();
		if ((!isColFormatKnown() && cellsCount > 0) || (isColFormatKnown() && cellsCount % colsAttribute.size() != 0)) {
			builder.endBlock(); // close table row
		}
		builder.endBlock(); // close table
	}

	private void closeCellBlockIfNeeded() {
		if (cellBlockIsOpen) {
			builder.endBlock(); // close table cell
			cellBlockIsOpen = false;

			cellsCount = cellsCount + 1;

			if (!colsAttribute.isEmpty() && cellsCount % colsAttribute.size() == 0) {
				builder.endBlock(); // close table row
			}
		}
	}
}
