/*******************************************************************************
 * Copyright (c) 2016 Obeo
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cedric Brun - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.wikitext.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.swt.dnd.Clipboard;

/**
 * A Paste preprocessor might prepare the content available on the clipboard instance to adapt it in a form which is
 * more suitable to the Markup editor.
 *
 * @author Cedric Brun
 * @since 2.11
 */
public interface PastePreprocessor {

	/**
	 * a No-Op preprocessor which does nothing.
	 */
	PastePreprocessor NOOP = new PastePreprocessor() {

		@Override
		public void setMarkupLanguage(MarkupLanguage markupLanguage) {
			// ignore

		}

		@Override
		public void setFile(IFile file) {
			// ignore

		}

		@Override
		public Runnable prepareClipboard(Clipboard clipboard) {
			// ignore
			return null;

		}
	};

	/**
	 * Prepare for pasting the content which might be found in the cliboard.
	 *
	 * @param clipboard
	 *            a clipboard instance.
	 * @return a runnable which has to revert the clipboard content to the original state, the one before the
	 *         preprocessor adapted the content.
	 */

	Runnable prepareClipboard(Clipboard clipboard);

	/**
	 * Called by the editor to indicate to the preprocessor which Markup file is currently being edited. Please note
	 * that the edited markup content might not be from a file and as such this method might not be called.
	 *
	 * @param file
	 *            the file currently being edited.
	 */
	void setFile(IFile file);

	/**
	 * Called by the editor to indicate which markup language is currently being used for the content.
	 *
	 * @param markupLanguage
	 *            a markup language.
	 */
	void setMarkupLanguage(MarkupLanguage markupLanguage);

}
