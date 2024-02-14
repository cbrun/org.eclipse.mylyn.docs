package org.eclipse.mylyn.internal.wikitext.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.mylyn.wikitext.parser.markup.MarkupLanguage;
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
		public Runnable prepareClipboard(Clipboard clipboard) {
			return new Runnable() {

				@Override
				public void run() {
					// do nothing by default
				}
			};

		}
	};

	Runnable prepareClipboard(Clipboard clipboard);

	void setFile(IFile file);

	void setMarkupLanguage(MarkupLanguage markupLanguage);

}