/*******************************************************************************
 * Copyright (c) 2007, 2015 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *     Jeremie Bresson - Bug 473109
 *******************************************************************************/
package org.eclipse.mylyn.internal.wikitext.mediawiki.core.token;

import org.eclipse.mylyn.wikitext.parser.markup.PatternBasedElement;
import org.eclipse.mylyn.wikitext.parser.markup.PatternBasedElementProcessor;

/**
 * Match external links [http://example.org/ example site].
 *
 * @author David Green
 */
public class HyperlinkExternalReplacementToken extends PatternBasedElement {

	@Override
	protected String getPattern(int groupOffset) {
		return "(?:\\[((mailto\\:|[a-zA-Z]+\\://)[^\\[\\]\\|\\s]+)(?:(?:\\||\\s)([^\\]]*))?\\s*\\])"; //$NON-NLS-1$
	}

	@Override
	protected int getPatternGroupCount() {
		return 3;
	}

	@Override
	protected PatternBasedElementProcessor newProcessor() {
		return new HyperlinkReplacementTokenProcessor();
	}

	private static class HyperlinkReplacementTokenProcessor extends PatternBasedElementProcessor {
		@Override
		public void emit() {
			String href = group(1);
			String altText = group(3);

			if (altText == null || altText.trim().length() == 0) {
				altText = href;
			}
			builder.link(href, altText);
		}
	}

}
