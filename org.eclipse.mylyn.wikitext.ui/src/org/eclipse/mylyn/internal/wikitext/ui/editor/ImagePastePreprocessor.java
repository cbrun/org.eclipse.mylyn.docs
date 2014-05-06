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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.mylyn.internal.wikitext.ui.WikiTextUiPlugin;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.ImageAttributes;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.html.core.HtmlLanguage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

public class ImagePastePreprocessor implements PastePreprocessor {

	private static final String PNG_EXTENSION = ".png"; //$NON-NLS-1$

	private static final Pattern ILLEGAL_CHARACTERE = Pattern.compile("[^\\w_\\-\\.]"); //$NON-NLS-1$

	private IFile file;

	private MarkupLanguage markup;

	private final ISelectionProvider selectionProvider;

	public ImagePastePreprocessor(ISelectionProvider selectionProvider) {
		this.selectionProvider = selectionProvider;
	}

	@Override
	public void prepareClipboard(Clipboard clipboard) {
		ImageData imageData = (ImageData) clipboard.getContents(ImageTransfer.getInstance());

		if (imageData != null && imageData.data != null && imageData.data.length > 0 && file != null && markup != null) {
			ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { imageData };
			HashFunction hf = Hashing.md5();
			HashCode code = hf.hashBytes(imageData.data);

			String defaultLabelFromTextText = getDefaultLabelFromTextText();
			IFile newImageFile = getNewImageFile(defaultLabelFromTextText != null
					? defaultLabelFromTextText
					: code.toString());

			if (newImageFile == null) {
				//If canceled prevent past
				clipboard.clearContents();
				return;
			}
			File imageFile = new File(newImageFile.getLocation().toOSString());
			//Save previous choice from next time
			storeSettings(newImageFile);
			try {
				Files.createParentDirs(imageFile);
				imageLoader.save(imageFile.getAbsolutePath(), SWT.IMAGE_PNG);
				StringWriter out = new StringWriter();
				DocumentBuilder builder = markup.createDocumentBuilder(out);
				ImageAttributes imgAttr = new ImageAttributes();
				imgAttr.setWidth(imageData.width);
				imgAttr.setHeight(imageData.height);
				builder.image(imgAttr, createRelativePath(newImageFile));
				TextTransfer textTransfer = TextTransfer.getInstance();
				clipboard.setContents(new Object[] { imageData, out.toString() },
						new Transfer[] { ImageTransfer.getInstance(), textTransfer });
			} catch (IOException e) {
				/*
				 * This is really some kind of extra assist. Anything goes wrong, then we won't do a thing and don't want to bother the end user.
				 */
			} catch (UnsupportedOperationException e) {
				/*
				 * the current markup langage does not support the document builder interface.
				 */
			}
			try {
				file.getParent().refreshLocal(2, new NullProgressMonitor());
			} catch (CoreException e) {
				/*
				 * This is really some kind of extra assist. Anything goes wrong, then we won't do a thing and don't want to bother the end user.
				 */
			}
		}

		String htmlText = (String) clipboard.getContents(HTMLTransfer.getInstance());
		if (htmlText != null) {
			StringWriter out = new StringWriter();
			HtmlLanguage language = new HtmlLanguage();
			language.setParseCleansHtml(true);
			MarkupParser markupParser = new MarkupParser(language, new NoStyleDocumentBuilder(
					markup.createDocumentBuilder(out)));
			markupParser.parse(htmlText, false);
			clipboard.setContents(new Object[] { htmlText, out.toString() },
					new Transfer[] { HTMLTransfer.getInstance(), TextTransfer.getInstance() });
		}
	}

	private void storeSettings(IFile newImageFile) {
		IDialogSettings settings = getDialogSettings();
		settings.put(file.getFullPath().toPortableString(), newImageFile.getParent().getFullPath().toPortableString());
	}

	private IDialogSettings getDialogSettings() {
		IDialogSettings dialogSettings = WikiTextUiPlugin.getDefault().getDialogSettings();
		IDialogSettings settings = dialogSettings.getSection(WorkspaceResourceDialog.class.getName());
		if (settings == null) {
			settings = dialogSettings.addNewSection(WorkspaceResourceDialog.class.getName());
		}
		return settings;
	}

	private String getPrevisouChoice() {
		return getDialogSettings().get(file.getFullPath().toPortableString());
	}

	private IFile getNewImageFile(String defaultName) {
		String defaultPath = getPrevisouChoice();
		if (!defaultName.endsWith(PNG_EXTENSION)) {
			defaultName = defaultName + PNG_EXTENSION;
		}

		IPath suggestedPath = defaultPath != null ? new Path(defaultPath) : file.getParent().getFullPath();
		IFile newImageFile = WorkspaceResourceDialog.openNewFile(Display.getCurrent().getActiveShell(), "Image folder",
				"Select the image folder", suggestedPath, defaultName, file.getProject());
		return newImageFile;
	}

	private String createRelativePath(IFile newImageFile) {
		return newImageFile.getFullPath().makeRelativeTo(file.getParent().getFullPath()).toPortableString();
	}

	@Override
	public void setFile(IFile file) {
		this.file = file;

	}

	@Override
	public void setMarkupLanguage(MarkupLanguage markupLanguage) {
		this.markup = markupLanguage;
	}

	private String getDefaultLabelFromTextText() {
		if (selectionProvider != null) {
			ISelection selection = selectionProvider.getSelection();
			if (selection instanceof TextSelection) {
				TextSelection textSelection = (TextSelection) selection;
				String text = textSelection.getText();
				return correctFileName(text);
			}
		}
		return null;
	}

	public static String correctFileName(final String init) {
		if (init == null) {
			return null;
		}
		//Remove illegal character in name
		Matcher matcher = ILLEGAL_CHARACTERE.matcher(init);
		String correctLabel = matcher.replaceAll("_"); //$NON-NLS-1$
		return correctLabel.length() > 0 ? correctLabel : null;
	}

}
