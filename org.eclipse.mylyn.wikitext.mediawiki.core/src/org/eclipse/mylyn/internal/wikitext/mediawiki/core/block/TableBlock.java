/*******************************************************************************
 * Copyright (c) 2007, 2011 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.internal.wikitext.mediawiki.core.block;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder.BlockType;
import org.eclipse.mylyn.wikitext.core.parser.TableAttributes;
import org.eclipse.mylyn.wikitext.core.parser.TableCellAttributes;
import org.eclipse.mylyn.wikitext.core.parser.TableRowAttributes;
import org.eclipse.mylyn.wikitext.core.parser.markup.Block;

/**
 * an implementation of MediaWiki tables, see <a
 * href="http://www.mediawiki.org/wiki/Help:Tables">MediaWiki:Help:Tables</a> for details
 * 
 * @author David Green
 * @author Daniel Migowski bug 274730 tables having mixed headers/normal cells
 */
public class TableBlock extends Block {

	private static final Pattern rowCellSplitter = Pattern.compile("\\s*(\\|\\||!!)\\s*"); //$NON-NLS-1$

	private static final Pattern startPattern = Pattern.compile("\\{\\|\\s*(.+)?"); //$NON-NLS-1$

	private static final Pattern optionsPattern = Pattern.compile("([a-zA-Z]+)=\"([^\"]*)\""); //$NON-NLS-1$

	private static final Pattern newRowPattern = Pattern.compile("\\|-\\s*(.+)?"); //$NON-NLS-1$

	private static final Pattern cellPattern = Pattern.compile("(\\||!)\\s*(.+)?"); //$NON-NLS-1$

	private static final Pattern cellSplitterPattern = Pattern.compile("\\s*(?:([^\\|\\[]+)?\\|)?\\s*(.+)?"); //$NON-NLS-1$

	private static final Pattern endPattern = Pattern.compile("\\|\\}\\s*(.+)?"); //$NON-NLS-1$

	private int blockLineCount;

	private Matcher matcher;

	private boolean openRow;

	@Override
	public int processLineContent(String line, int offset) {
		if (blockLineCount++ == 0) {
			TableAttributes attributes = new TableAttributes();

			// first line opens table
			String options = matcher.group(1);
			if (options != null) {
				Matcher optionsMatcher = optionsPattern.matcher(options);
				while (optionsMatcher.find()) {
					String optionName = optionsMatcher.group(1);
					String optionValue = optionsMatcher.group(2);
					if (optionName.equalsIgnoreCase("id")) { //$NON-NLS-1$
						attributes.setId(optionValue);
					} else if (optionName.equalsIgnoreCase("style")) { //$NON-NLS-1$
						attributes.setCssStyle(optionValue);
					} else if (optionName.equalsIgnoreCase("class")) { //$NON-NLS-1$
						attributes.setCssClass(optionValue);
					} else if (optionName.equalsIgnoreCase("title")) { //$NON-NLS-1$
						attributes.setTitle(optionValue);
					} else if (optionName.equalsIgnoreCase("border")) { //$NON-NLS-1$
						attributes.setBorder(optionValue);
					} else if (optionName.equalsIgnoreCase("summary")) { //$NON-NLS-1$
						attributes.setSummary(optionValue);
					} else if (optionName.equalsIgnoreCase("width")) { //$NON-NLS-1$
						attributes.setWidth(optionValue);
					} else if (optionName.equalsIgnoreCase("frame")) { //$NON-NLS-1$
						attributes.setFrame(optionValue);
					} else if (optionName.equalsIgnoreCase("rules")) { //$NON-NLS-1$
						attributes.setRules(optionValue);
					} else if (optionName.equalsIgnoreCase("cellspacing")) { //$NON-NLS-1$
						attributes.setCellspacing(optionValue);
					} else if (optionName.equalsIgnoreCase("cellpadding")) { //$NON-NLS-1$
						attributes.setCellpadding(optionValue);
					} else if (optionName.equalsIgnoreCase("bgcolor")) { //$NON-NLS-1$
						attributes.setBgcolor(optionValue);
					}
				}
			}
			builder.beginBlock(BlockType.TABLE, attributes);
			// table open line never has cells
			return -1;
		} else {
			Matcher newRowMatcher = newRowPattern.matcher(line);
			if (newRowMatcher.matches()) {
				TableRowAttributes attributes = new TableRowAttributes();
				String newRowOptions = newRowMatcher.group(1);
				if (newRowOptions != null) {
					Matcher optionsMatcher = optionsPattern.matcher(newRowOptions);
					while (optionsMatcher.find()) {
						String optionName = optionsMatcher.group(1);
						String optionValue = optionsMatcher.group(2);
						if (optionName.equalsIgnoreCase("id")) { //$NON-NLS-1$
							attributes.setId(optionValue);
						} else if (optionName.equalsIgnoreCase("style")) { //$NON-NLS-1$
							attributes.setCssStyle(optionValue);
						} else if (optionName.equalsIgnoreCase("class")) { //$NON-NLS-1$
							attributes.setCssClass(optionValue);
						} else if (optionName.equalsIgnoreCase("title")) { //$NON-NLS-1$
							attributes.setTitle(optionValue);
						} else if (optionName.equalsIgnoreCase("align")) { //$NON-NLS-1$
							attributes.setAlign(optionValue);
						} else if (optionName.equalsIgnoreCase("valign")) { //$NON-NLS-1$
							attributes.setValign(optionValue);
						} else if (optionName.equalsIgnoreCase("bgcolor")) { //$NON-NLS-1$
							attributes.setBgcolor(optionValue);
						}
					}
				}
				openRow(newRowMatcher.start(), attributes);
				return -1;
			} else {
				Matcher endMatcher = endPattern.matcher(line);
				if (endMatcher.matches()) {
					setClosed(true);
					return endMatcher.start(1);
				} else {

					Matcher cellMatcher = cellPattern.matcher(line);
					if (cellMatcher.matches()) {
						String kind = cellMatcher.group(1);
						String contents = cellMatcher.group(2);
						if (contents == null) {
							// likely an incomplete line
							return -1;
						}
						int contentsStart = cellMatcher.start(2);
						BlockType type = ("!".equals(kind)) ? BlockType.TABLE_CELL_HEADER : BlockType.TABLE_CELL_NORMAL; //$NON-NLS-1$

						if (!openRow) {
							openRow(cellMatcher.start(), new Attributes());
						}
						emitCells(contentsStart, type, contents);

						return -1;
					} else {
						// ignore, bad formatting or unsupported syntax (caption)
						return -1;
					}
				}
			}
		}
	}

	private void emitCells(int contentsStart, BlockType type, String contents) {
		int lastEnd = 0;
		Matcher matcher = rowCellSplitter.matcher(contents);
		while (matcher.find()) {
			int found = matcher.start();
			String cell;
			if (found > lastEnd) {
				cell = contents.substring(lastEnd, found);
			} else {
				cell = ""; //$NON-NLS-1$
			}
			emitCell(lastEnd + contentsStart, type, cell);

			// Depending on the cell splitter the next cell is either a
			// header or normal cell.
			String splitterSpec = matcher.group(1);
			if ("!!".equals(splitterSpec)) { //$NON-NLS-1$
				type = BlockType.TABLE_CELL_HEADER;
			} else {
				type = BlockType.TABLE_CELL_NORMAL;
			}

			lastEnd = matcher.end();
		}
		if (lastEnd < contents.length()) {
			emitCell(lastEnd + contentsStart, type, contents.substring(lastEnd));
		}
	}

	private void emitCell(int lineCharacterOffset, BlockType type, String cell) {
		Matcher cellSplitterMatcher = cellSplitterPattern.matcher(cell);
		if (!cellSplitterMatcher.matches()) {
			throw new IllegalStateException();
		}
		String cellOptions = cellSplitterMatcher.group(1);
		String cellContents = cellSplitterMatcher.group(2);
		if (cellContents == null) {
			// probably invalid syntax
			return;
		}

		int contentsStart = cellSplitterMatcher.start(2);

		TableCellAttributes attributes = new TableCellAttributes();

		if (cellOptions != null) {
			Matcher optionsMatcher = optionsPattern.matcher(cellOptions);
			while (optionsMatcher.find()) {
				String optionName = optionsMatcher.group(1);
				String optionValue = optionsMatcher.group(2);
				if (optionName.equalsIgnoreCase("id")) { //$NON-NLS-1$
					attributes.setId(optionValue);
				} else if (optionName.equalsIgnoreCase("style")) { //$NON-NLS-1$
					attributes.setCssStyle(optionValue);
				} else if (optionName.equalsIgnoreCase("class")) { //$NON-NLS-1$
					attributes.setCssClass(optionValue);
				} else if (optionName.equalsIgnoreCase("title")) { //$NON-NLS-1$
					attributes.setTitle(optionValue);
				} else if (optionName.equalsIgnoreCase("align")) { //$NON-NLS-1$
					attributes.setAlign(optionValue);
				} else if (optionName.equalsIgnoreCase("valign")) { //$NON-NLS-1$
					attributes.setValign(optionValue);
				} else if (optionName.equalsIgnoreCase("bgcolor")) { //$NON-NLS-1$
					attributes.setBgcolor(optionValue);
				} else if (optionName.equalsIgnoreCase("colspan")) { //$NON-NLS-1$
					attributes.setColspan(optionValue);
				} else if (optionName.equalsIgnoreCase("rowspan")) { //$NON-NLS-1$
					attributes.setRowspan(optionValue);
				}
			}
		}
		state.setLineCharacterOffset(lineCharacterOffset);

		builder.beginBlock(type, attributes);

		markupLanguage.emitMarkupLine(parser, state, lineCharacterOffset + contentsStart, cellContents, 0);
		builder.endBlock();
	}

	private void openRow(int lineOffset, Attributes attributes) {
		closeRow();
		state.setLineCharacterOffset(lineOffset);
		builder.beginBlock(BlockType.TABLE_ROW, attributes);
		openRow = true;
	}

	private void closeRow() {
		if (openRow) {
			builder.endBlock();
			openRow = false;
		}
	}

	@Override
	public boolean canStart(String line, int lineOffset) {
		blockLineCount = 0;
		openRow = false;
		if (lineOffset == 0) {
			matcher = startPattern.matcher(line);
			return matcher.matches();
		} else {
			matcher = null;
			return false;
		}
	}

	@Override
	public void setClosed(boolean closed) {
		if (closed && !isClosed()) {
			closeRow();
			builder.endBlock();
		}
		super.setClosed(closed);
	}

}
