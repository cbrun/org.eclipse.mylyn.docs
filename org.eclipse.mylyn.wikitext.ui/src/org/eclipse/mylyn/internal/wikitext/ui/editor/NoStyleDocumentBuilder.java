/*******************************************************************************
 * Copyright (c) 2014 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.wikitext.ui.editor;

import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.LinkAttributes;

public class NoStyleDocumentBuilder extends DocumentBuilder {

	private final DocumentBuilder delegate;

	private boolean currentlyIgnoringSpan;

/*
 * When pasting some HTML documents tend to start with a span which is going to go through all the document. We prefer to ignore that.
 */
	private boolean hadNonSpanAlready = false;

	public NoStyleDocumentBuilder(DocumentBuilder delegate) {
		this.delegate = delegate;
	}

	@Override
	public void beginDocument() {
		this.delegate.beginDocument();
		hadNonSpanAlready = true;

	}

	@Override
	public void endDocument() {
		this.delegate.endDocument();
		hadNonSpanAlready = true;

	}

	@Override
	public void beginBlock(BlockType type, Attributes attributes) {
		this.delegate.beginBlock(type, clean(attributes));
		hadNonSpanAlready = true;

	}

	@Override
	public void endBlock() {
		this.delegate.endBlock();

	}

	@Override
	public void beginSpan(SpanType type, Attributes attributes) {
		if (type != SpanType.SPAN && hadNonSpanAlready) {
			this.delegate.beginSpan(type, clean(attributes));
		} else {
			currentlyIgnoringSpan = true;
		}

	}

	@Override
	public void endSpan() {
		if (currentlyIgnoringSpan) {
			currentlyIgnoringSpan = false;
		} else {
			this.delegate.endSpan();
		}

	}

	@Override
	public void beginHeading(int level, Attributes attributes) {
		hadNonSpanAlready = true;
		this.delegate.beginHeading(level, clean(attributes));

	}

	@Override
	public void endHeading() {
		this.delegate.endHeading();
	}

	@Override
	public void characters(String text) {
		hadNonSpanAlready = true;
		this.delegate.characters(text);

	}

	@Override
	public void entityReference(String entity) {
		hadNonSpanAlready = true;
		this.delegate.entityReference(entity);

	}

	@Override
	public void image(Attributes attributes, String url) {
		hadNonSpanAlready = true;
		this.delegate.image(clean(attributes), url);

	}

	@Override
	public void link(Attributes attributes, String hrefOrHashName, String text) {
		hadNonSpanAlready = true;
		this.delegate.link(clean(attributes), hrefOrHashName, text);
	}

	private Attributes clean(Attributes attributes) {
		hadNonSpanAlready = true;
		attributes.setCssClass(null);
		attributes.setCssStyle(null);
		attributes.setId(null);
		if (attributes instanceof LinkAttributes) {
			((LinkAttributes) attributes).setTarget(null);
		}
		return attributes;
	}

	@Override
	public void imageLink(Attributes linkAttributes, Attributes imageAttributes, String href, String imageUrl) {
		hadNonSpanAlready = true;
		this.delegate.imageLink(clean(linkAttributes), clean(imageAttributes), href, imageUrl);

	}

	@Override
	public void acronym(String text, String definition) {
		hadNonSpanAlready = true;
		this.delegate.acronym(text, definition);

	}

	@Override
	public void lineBreak() {
		this.delegate.lineBreak();

	}

	@Override
	public void charactersUnescaped(String literal) {
		hadNonSpanAlready = true;
		this.delegate.charactersUnescaped(literal);

	}

}
