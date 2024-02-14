package org.eclipse.mylyn.wikitext.ui.editor;

import org.eclipse.mylyn.wikitext.parser.Attributes;
import org.eclipse.mylyn.wikitext.parser.DocumentBuilder;
import org.eclipse.mylyn.wikitext.parser.LinkAttributes;

public class NoStyleDocumentBuilder extends DocumentBuilder {

	private final DocumentBuilder delegate;

/*
 * When pasting some HTML documents tend to start with a span which is going to go through all the document. We prefer to ignore that.
 */

	public NoStyleDocumentBuilder(DocumentBuilder delegate) {
		this.delegate = delegate;
	}

	@Override
	public void beginDocument() {
		this.delegate.beginDocument();

	}

	@Override
	public void endDocument() {
		this.delegate.endDocument();

	}

	@Override
	public void beginBlock(BlockType type, Attributes attributes) {
		this.delegate.beginBlock(type, clean(attributes));

	}

	@Override
	public void endBlock() {
		this.delegate.endBlock();

	}

	@Override
	public void beginSpan(SpanType type, Attributes attributes) {
		this.delegate.beginSpan(type, clean(attributes));

	}

	@Override
	public void endSpan() {
		this.delegate.endSpan();

	}

	@Override
	public void beginHeading(int level, Attributes attributes) {
		this.delegate.beginHeading(level, clean(attributes));

	}

	@Override
	public void endHeading() {
		this.delegate.endHeading();
	}

	@Override
	public void characters(String text) {
		this.delegate.characters(text);

	}

	@Override
	public void entityReference(String entity) {
		this.delegate.entityReference(entity);

	}

	@Override
	public void image(Attributes attributes, String url) {
		this.delegate.image(clean(attributes), url);

	}

	@Override
	public void link(Attributes attributes, String hrefOrHashName, String text) {
		this.delegate.link(clean(attributes), hrefOrHashName, text);
	}

	private Attributes clean(Attributes attributes) {
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
		this.delegate.imageLink(clean(linkAttributes), clean(imageAttributes), href, imageUrl);

	}

	@Override
	public void acronym(String text, String definition) {
		this.delegate.acronym(text, definition);

	}

	@Override
	public void lineBreak() {
		this.delegate.lineBreak();

	}

	@Override
	public void charactersUnescaped(String literal) {
		this.delegate.charactersUnescaped(literal);

	}

}