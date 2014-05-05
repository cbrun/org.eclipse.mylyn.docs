/*******************************************************************************
 * Copyright (c) 2014 Obeo
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cedric Brun - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.wikitext.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.swt.dnd.Clipboard;

public interface PastePreprocessor {

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
		public void prepareClipboard(Clipboard clipboard) {
			// ignore

		}
	};

	void prepareClipboard(Clipboard clipboard);

	void setFile(IFile file);

	void setMarkupLanguage(MarkupLanguage markupLanguage);

}
