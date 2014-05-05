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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
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

import com.google.common.base.StandardSystemProperty;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

public class ImagePastePreprocessor implements PastePreprocessor {

	private IFile file;

	private MarkupLanguage markup;

	@Override
	public void prepareClipboard(Clipboard clipboard) {
		ImageData imageData = (ImageData) clipboard.getContents(ImageTransfer.getInstance());

		if (imageData != null && imageData.data != null && imageData.data.length > 0 && file != null && markup != null) {
			ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { imageData };
			String IMG_FOLDER_NAME = "pasted-images";
			HashFunction hf = Hashing.md5();
			HashCode code = hf.hashBytes(imageData.data);

			String imageFileName = code.toString() + ".png"; //$NON-NLS-1$
			File imageFile = new File(file.getParent()
					.getLocation()
					.append(IMG_FOLDER_NAME)
					.append(imageFileName)
					.toOSString());

			try {
				Files.createParentDirs(imageFile);
				imageLoader.save(imageFile.getAbsolutePath(), SWT.IMAGE_PNG);
				StringWriter out = new StringWriter();
				DocumentBuilder builder = markup.createDocumentBuilder(out);
				ImageAttributes imgAttr = new ImageAttributes();
				imgAttr.setWidth(imageData.width);
				imgAttr.setHeight(imageData.height);
				builder.image(imgAttr, IMG_FOLDER_NAME + StandardSystemProperty.FILE_SEPARATOR.value() + imageFileName);
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

	@Override
	public void setFile(IFile file) {
		this.file = file;

	}

	@Override
	public void setMarkupLanguage(MarkupLanguage markupLanguage) {
		this.markup = markupLanguage;

	}

}
