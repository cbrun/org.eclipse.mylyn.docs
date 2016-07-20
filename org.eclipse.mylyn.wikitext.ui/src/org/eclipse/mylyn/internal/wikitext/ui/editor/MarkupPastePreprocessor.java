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

package org.eclipse.mylyn.internal.wikitext.ui.editor;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
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
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.ui.editor.PastePreprocessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * A paste preprocessor for the markup editor. It reacts to an image buffer present in the clipboard by creating the
 * corresponding file in the workspace and pasting the markup referencing this file.
 *
 * @author Cedric Brun
 */
public class MarkupPastePreprocessor implements PastePreprocessor {

	private static final String PNG_EXTENSION = ".png"; //$NON-NLS-1$

	private static final Pattern ILLEGAL_CHARACTERE = Pattern.compile("[^\\w_\\-\\.]"); //$NON-NLS-1$

	private IFile file;

	private MarkupLanguage markup;

	private final ISelectionProvider selectionProvider;

	public MarkupPastePreprocessor(ISelectionProvider selectionProvider) {
		this.selectionProvider = selectionProvider;
	}

	@Override
	public Runnable prepareClipboard(Clipboard clipboard) {
		ImageData imageData = (ImageData) clipboard.getContents(ImageTransfer.getInstance());

		final Object oldTextClipboard = clipboard.getContents(TextTransfer.getInstance());
		final Object oldImageClipboard = clipboard.getContents(ImageTransfer.getInstance());
		final Object oldHtmlClipboard = clipboard.getContents(HTMLTransfer.getInstance());
		Runnable resetClipboardState = new Runnable() {

			@Override
			public void run() {
				try {

					ArrayList<Object> objects = Lists.newArrayList();
					ArrayList<Transfer> transfers = Lists.newArrayList();
					if (oldTextClipboard != null) {
						objects.add(oldTextClipboard);
						transfers.add(TextTransfer.getInstance());
					}
					if (oldHtmlClipboard != null) {
						objects.add(oldHtmlClipboard);
						transfers.add(HTMLTransfer.getInstance());
					}
					if (oldImageClipboard != null) {
						objects.add(oldImageClipboard);
						transfers.add(ImageTransfer.getInstance());
					}

					Object[] objArray = new Object[objects.size()];
					Transfer[] transArray = new Transfer[transfers.size()];
					objects.toArray(objArray);
					transfers.toArray(transArray);
					clipboard.setContents(objArray, transArray);
				} finally {
					clipboard.dispose();
				}
			}
		};
		if (imageData != null && imageData.data != null && imageData.data.length > 0 && file != null
				&& markup != null) {
			ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { imageData };
			HashFunction hf = Hashing.md5();
			HashCode code = hf.hashBytes(imageData.data);

			String defaultLabelFromTextText = getDefaultLabelFromTextText();
			IFile newImageFile = getNewImageFile(
					defaultLabelFromTextText != null ? defaultLabelFromTextText : code.toString());

			if (newImageFile == null) {
				/*
				 * user pressed "cancel" hence no target file
				 */
				clipboard.clearContents();
				return resetClipboardState;
			}
			File imageFile = new File(newImageFile.getLocation().toOSString());
			//Save previous choice from next time
			storeSettings(newImageFile);
			try {
				Files.createParentDirs(imageFile);
				imageLoader.save(imageFile.getAbsolutePath(), SWT.IMAGE_PNG);
				StringWriter out = new StringWriter();
				try {
					DocumentBuilder builder = markup.createDocumentBuilder(out);
					ImageAttributes imgAttr = new ImageAttributes();
					imgAttr.setWidth(imageData.width);
					imgAttr.setHeight(imageData.height);
					builder.image(imgAttr, createRelativePath(newImageFile));
					builder.flush();
				} catch (UnsupportedOperationException e) {
					/*
					 * the current markup langage does not support the document builder interface, let's at least paste the image uri.
					 */
					out.append(createRelativePath(newImageFile));
				}

				String toPaste = out.toString();
				if (toPaste.length() == 0) {
					toPaste = createRelativePath(newImageFile);
				}
				clipboard.setContents(new Object[] { imageData, toPaste },
						new Transfer[] { ImageTransfer.getInstance(), TextTransfer.getInstance() });
			} catch (IOException e) {
				WikiTextUiPlugin.getDefault().log(e);
			}
			/*
			 * Auto-adapting copy-paste is really some kind of extra assist. Anything goes wrong, then we won't do a thing and don't want to bother the end user.
			 */
			try {
				file.getParent().refreshLocal(2, new NullProgressMonitor());
			} catch (CoreException core) {
				/*
				 * Auto-adapting copy-paste is really some kind of extra assist. Anything goes wrong, then we won't do a thing and don't want to bother the end user.
				 */
				WikiTextUiPlugin.getDefault().log(core);
			}
		}

		return resetClipboardState;
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
		IFile newImageFile = WorkspaceResourceDialog.openNewFile(Display.getCurrent().getActiveShell(),
				Messages.MarkupEditor_imagesFolder, Messages.MarkupEditor_imagesFolder_select, suggestedPath,
				defaultName, file.getProject());
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

	private static String correctFileName(final String init) {
		if (init == null) {
			return null;
		}
		//Remove illegal character in name
		Matcher matcher = ILLEGAL_CHARACTERE.matcher(init);
		String correctLabel = matcher.replaceAll("_"); //$NON-NLS-1$
		return correctLabel.length() > 0 ? correctLabel : null;
	}

}
