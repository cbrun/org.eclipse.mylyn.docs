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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.eclipse.mylyn.wikitext.html.HtmlLanguage;
import org.eclipse.mylyn.wikitext.parser.DocumentBuilder;
import org.eclipse.mylyn.wikitext.parser.ImageAttributes;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.ui.editor.NoStyleDocumentBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

import com.google.common.io.Files;

public class ImagePastePreprocessor implements PastePreprocessor {

	private IFile file;

	private MarkupLanguage markup;

	private static final String PNG_EXTENSION = ".png"; //$NON-NLS-1$

	private final ISelectionProvider selectionProvider;

	private static final Pattern ILLEGAL_CHARACTERE = Pattern.compile("[^\\w_\\-\\.]"); //$NON-NLS-1$

	public ImagePastePreprocessor(ISelectionProvider selectionProvider) {
		this.selectionProvider = selectionProvider;
	}

	private static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (byte element : hash) {
			String hex = Integer.toHexString(0xff & element);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	@Override
	public Runnable prepareClipboard(Clipboard clipboard) {
		final Object oldTextClipboard = clipboard.getContents(TextTransfer.getInstance());
		final Object oldHtmlClipboard = clipboard.getContents(HTMLTransfer.getInstance());
		Runnable resetClipboardState = new Runnable() {

			@Override
			public void run() {
				try {
					if (oldHtmlClipboard == null) {
						clipboard.setContents(new Object[] { oldTextClipboard },
								new Transfer[] { TextTransfer.getInstance() });
					} else if (oldTextClipboard == null && oldHtmlClipboard != null) {
						clipboard.setContents(new Object[] { oldHtmlClipboard },
								new Transfer[] { HTMLTransfer.getInstance() });
					} else {
						clipboard.setContents(new Object[] { oldTextClipboard, oldHtmlClipboard },
								new Transfer[] { TextTransfer.getInstance(), HTMLTransfer.getInstance() });
					}
				} finally {
					clipboard.dispose();
				}
			}
		};
		ImageData imageData = (ImageData) clipboard.getContents(ImageTransfer.getInstance());

		if (imageData != null && imageData.data != null && imageData.data.length > 0 && file != null
				&& markup != null) {
			try {
				ImageLoader imageLoader = new ImageLoader();
				imageLoader.data = new ImageData[] { imageData };

				MessageDigest digest = MessageDigest.getInstance("MD5");
				final byte[] hashbytes = digest.digest(imageData.data);
				String code = bytesToHex(hashbytes);

				String defaultLabelFromTextText = getDefaultLabelFromTextText();
				IFile newImageFile = getNewImageFile(
						defaultLabelFromTextText != null ? defaultLabelFromTextText : code.toString());

				if (newImageFile == null) {
					//If canceled prevent past
					clipboard.clearContents();
					return resetClipboardState;
				}
				File imageFile = new File(newImageFile.getLocation().toOSString());
				//Save previous choice from next time
				storeSettings(newImageFile);

				Files.createParentDirs(imageFile);
				imageLoader.save(imageFile.getAbsolutePath(), SWT.IMAGE_PNG);
				StringWriter out = new StringWriter();
				DocumentBuilder builder = markup.createDocumentBuilder(out);
				ImageAttributes imgAttr = new ImageAttributes();
				imgAttr.setWidth(imageData.width);
				imgAttr.setHeight(imageData.height);
				builder.image(imgAttr, createRelativePath(newImageFile));
				builder.flush();
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
			} catch (NoSuchAlgorithmException e) {
				/*
				 *  hash algorighm is not there.
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
			MarkupParser markupParser = new MarkupParser(language,
					new NoStyleDocumentBuilder(markup.createDocumentBuilder(out)));
			markupParser.parse(htmlText, false);
			clipboard.setContents(new Object[] { htmlText, out.toString() },
					new Transfer[] { HTMLTransfer.getInstance(), TextTransfer.getInstance() });
		}
		return resetClipboardState;
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

	public static String correctFileName(final String init) {
		if (init == null) {
			return null;
		}
		//Remove illegal character in name
		Matcher matcher = ILLEGAL_CHARACTERE.matcher(init);
		String correctLabel = matcher.replaceAll("_"); //$NON-NLS-1$
		return correctLabel.length() > 0 ? correctLabel : null;
	}

	@Override
	public void setFile(IFile file) {
		this.file = file;

	}

	@Override
	public void setMarkupLanguage(MarkupLanguage markupLanguage) {
		this.markup = markupLanguage;

	}

}