/*******************************************************************************
 * Copyright (c) 2007, 2009 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.wikitext.ui.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.mylyn.internal.wikitext.ui.editor.FindAndReplaceTarget;
import org.eclipse.mylyn.internal.wikitext.ui.editor.PastePreprocessor;
import org.eclipse.mylyn.internal.wikitext.ui.editor.commands.ShowQuickOutlineCommand;
import org.eclipse.mylyn.internal.wikitext.ui.editor.syntax.FastMarkupPartitioner;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * A source viewer for editors using lightweight markup. Typically configured as follows:
 * 
 * <pre>
 * SourceViewer viewer = new MarkupSourceViewer(parent, null, style | SWT.WRAP, markupLanguage);
 * // configure the viewer
 * MarkupSourceViewerConfiguration configuration = createSourceViewerConfiguration(taskRepository, viewer);
 * 
 * configuration.setMarkupLanguage(markupLanguage);
 * configuration.setShowInTarget(new ShowInTargetBridge(viewer));
 * viewer.configure(configuration);
 * 
 * // we want the viewer to show annotations
 * viewer.showAnnotations(true);
 * </pre>
 * 
 * @author David Green
 * @since 1.1
 */
public class MarkupSourceViewer extends SourceViewer {
	private final MarkupLanguage markupLanguage;

	/**
	 * Operation code for quick outline
	 */
	public static final int QUICK_OUTLINE = ShowQuickOutlineCommand.QUICK_OUTLINE;

	private IInformationPresenter outlinePresenter;

	private IFindReplaceTarget findReplaceTarget;

	private PastePreprocessor preprocessor;

	public MarkupSourceViewer(Composite parent, IVerticalRuler ruler, int styles, MarkupLanguage markupLanguage) {
		super(parent, ruler, styles);
		this.markupLanguage = markupLanguage;
	}

	@Override
	public void setDocument(IDocument document, IAnnotationModel annotationModel, int modelRangeOffset,
			int modelRangeLength) {
		if (document != null) {
			configurePartitioning(document);
		}
		super.setDocument(document, annotationModel, modelRangeOffset, modelRangeLength);
	}

	private void configurePartitioning(IDocument document) {
		FastMarkupPartitioner partitioner = new FastMarkupPartitioner();
		partitioner.setMarkupLanguage(markupLanguage.clone());
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
	}

	@Override
	public boolean canDoOperation(int operation) {
		if (operation == QUICK_OUTLINE && outlinePresenter != null) {
			return true;
		}
		return super.canDoOperation(operation);
	}

	@Override
	public IFindReplaceTarget getFindReplaceTarget() {
		if (findReplaceTarget != null) {
			return findReplaceTarget;
		}
		return super.getFindReplaceTarget();
	}

	@Override
	public void configure(SourceViewerConfiguration configuration) {
		super.configure(configuration);
		if (configuration instanceof MarkupSourceViewerConfiguration) {
			MarkupSourceViewerConfiguration markupConfiguration = (MarkupSourceViewerConfiguration) configuration;
			outlinePresenter = markupConfiguration.getOutlineInformationPresenter(this);
			outlinePresenter.install(this);
			this.preprocessor = ((MarkupSourceViewerConfiguration) configuration).getPastePreprocessor();
			if (markupConfiguration.isEnableSelfContainedIncrementalFind()) {
				findReplaceTarget = new FindAndReplaceTarget(this);
			}
		}
	}

	@Override
	public void doOperation(int operation) {
		if (operation == QUICK_OUTLINE && outlinePresenter != null) {
			outlinePresenter.showInformation();
			return;
		}
		switch (operation) {
		case PASTE:
			if (preprocessor != null) {
				Clipboard clipboard = new Clipboard(getDisplay());
				try {
					preprocessor.prepareClipboard(clipboard);
				} finally {
					clipboard.dispose();
				}
			}

			break;
		}
		super.doOperation(operation);
	}

	/**
	 * Get the text widget's display.
	 * 
	 * @return the display or <code>null</code> if the display cannot be retrieved or if the display is disposed
	 * @since 3.0
	 */
	private Display getDisplay() {
		if (getControl() == null || getControl().isDisposed()) {
			return null;
		}

		Display display = getControl().getDisplay();
		if (display != null && display.isDisposed()) {
			return null;
		}

		return display;
	}
}