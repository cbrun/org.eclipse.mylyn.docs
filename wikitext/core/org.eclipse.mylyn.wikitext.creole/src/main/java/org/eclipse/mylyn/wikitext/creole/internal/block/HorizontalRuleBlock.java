/*******************************************************************************
 * Copyright (c) 2011, 2015 Igor Malinin and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Igor Malinin - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.creole.internal.block;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.mylyn.wikitext.parser.markup.Block;

/**
 * @author Igor Malinin
 */
public class HorizontalRuleBlock extends Block {

	private static final Pattern pattern = Pattern.compile("\\s*-{4}\\s*"); //$NON-NLS-1$

	private Matcher matcher;

	@Override
	public int processLineContent(String line, int offset) {
		builder.horizontalRule();
		setClosed(true);
		return -1;
	}

	@Override
	public boolean canStart(String line, int lineOffset) {
		if (lineOffset == 0) {
			matcher = pattern.matcher(line);
			return matcher.matches();
		} else {
			matcher = null;
			return false;
		}
	}

}
