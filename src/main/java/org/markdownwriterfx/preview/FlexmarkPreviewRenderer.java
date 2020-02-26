/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.markdownwriterfx.preview;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.Visitor;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;
import org.markdownwriterfx.addons.PreviewRendererAddon;
import org.markdownwriterfx.extensions.id.AllHtmlIdGenerator;
import org.markdownwriterfx.extensions.id.AutoIncrementIdAttributeProvider;
import org.markdownwriterfx.options.MarkdownExtensions;
import org.markdownwriterfx.util.Range;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;

/**
 * flexmark-java preview.
 *
 * @author Karl Tauber
 */
class FlexmarkPreviewRenderer
	implements MarkdownPreviewPane.Renderer {
	private static final ServiceLoader<PreviewRendererAddon> addons = ServiceLoader.load(PreviewRendererAddon.class);

	private String markdownText;
	private Node astRoot;
	private Node astRoot2;
	private Path path;

	private String htmlPreview;
	private String htmlSource;
	private String ast;

	@Override
	public String getHtml(Node node) {
		return getHtmlRenderer().render(node);
	}

	@Override
	public void update(String markdownText, Node astRoot, Path path) {
		assert markdownText != null;
		assert astRoot != null;

		if (this.astRoot == astRoot)
			return;

		this.markdownText = markdownText;
		this.astRoot = astRoot;
		this.path = path;

		astRoot2 = null;
		htmlPreview = null;
		htmlSource = null;
		ast = null;
	}

	@Override
	public String getHtml(boolean source) {
		if (source) {
			if (htmlSource == null)
				htmlSource = toHtml(true);
			return htmlSource;
		} else {
			if (htmlPreview == null)
				htmlPreview = toHtml(false);
			return htmlPreview;
		}
	}

	@Override
	public String getAST() {
		if (ast == null)
			ast = printTree();
		return ast;
	}

	@Override
	public List<Range> findSequences(int startOffset, int endOffset) {
		ArrayList<Range> sequences = new ArrayList<>();

		Node astRoot = toAstRoot();
		if (astRoot == null)
			return sequences;

		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			@Override
			protected void processNode(@NotNull Node node, boolean withChildren, @NotNull BiConsumer<Node, Visitor<Node>> processor) {
				BasedSequence chars = node.getChars();
				if (isInSequence(startOffset, endOffset, chars))
					sequences.add(new Range(chars.getStartOffset(), chars.getEndOffset()));

				for (BasedSequence segment : node.getSegments()) {
					if (isInSequence(startOffset, endOffset, segment))
						sequences.add(new Range(segment.getStartOffset(), segment.getEndOffset()));
				}

				visitChildren(node);
			}

		};
		visitor.visit(astRoot);
		return sequences;
	}

	private boolean isInSequence(int start, int end, BasedSequence sequence) {
		if (end == start)
			end++;
		return start < sequence.getEndOffset() && end > sequence.getStartOffset();
	}

	private Node parseMarkdown(String text) {
		Parser parser = Parser.builder()
			.extensions(MarkdownExtensions.getFlexmarkExtensions())
			.build();
		return parser.parse(text);
	}

	private Node toAstRoot() {
		if (!addons.iterator().hasNext())
			return astRoot; // no addons --> use AST from editor

		if (astRoot2 == null)
			astRoot2 = parseMarkdown(markdownText);
		return astRoot2;
	}

	HtmlRenderer htmlRenderer = null;

	public HtmlRenderer getHtmlRenderer() {
		if (htmlRenderer == null) {
			HtmlRenderer.Builder builder = HtmlRenderer.builder();
			MutableDataSet dataSet = new MutableDataSet();
			dataSet.set(EmojiExtension.ROOT_IMAGE_PATH, getClass().getResource("emojis/").toString());
			builder.setAll(dataSet);
			builder.attributeProviderFactory(new MyAttributeProvider.Factory())
				.attributeProviderFactory(new AutoIncrementIdAttributeProvider.Factory());
			builder.htmlIdGeneratorFactory(new AllHtmlIdGenerator.Factory());
			builder.extensions(MarkdownExtensions.getFlexmarkExtensions());

//		if (!source)
//			builder.attributeProviderFactory(new MyAttributeProvider.Factory());

			htmlRenderer = builder.build();

		}
		return htmlRenderer;
	}

	private String toHtml(boolean source) {
		Node astRoot;
		if (addons.iterator().hasNext()) {
			String text = markdownText;

			for (PreviewRendererAddon addon : addons)
				text = addon.preParse(text, path);

			astRoot = parseMarkdown(text);
		} else {
			// no addons --> use cached AST
			astRoot = toAstRoot();
		}

		if (astRoot == null)
			return "";
//		if (!source)
//			builder.attributeProviderFactory(new MyAttributeProvider.Factory());

		String html = getHtmlRenderer().render(astRoot);

		for (PreviewRendererAddon addon : addons)
			html = addon.postRender(html, path);


		return html;
	}


//	private boolean pn(Node node, Node node2, Map<Node, Node> diffs) {
//		if (node == null && node2 == null) {
//			return false;
//		}
//		if (node==null){
//
//		}
//		if (node.getChars().equals(node2.getChars())) {
//			return true;
//		}
//		// no same
//
//		boolean isDelOrReplace = node.getTextLength() >= node2.getTextLength();
//		if (isDelOrReplace) {
//			// pre le next
//			Node newChild = node.getFirstChild();
//			for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
//				// load child  compare with same child
//				if (pn(node, newChild, diffs)) {
//					newChild = newChild.getNext();
//				}
//				// not same
//
//			}
//
//
//		}
//
//
//		if (node.getChars().equals(node2.getChars())) {
//			return diffs;
//		}
//		diffs.put(node, node2);
//		return diffs;
//
//	}

	private String printTree() {
		Node astRoot = toAstRoot();
		if (astRoot == null)
			return "";

		StringBuilder buf = new StringBuilder(100);
		printNode(buf, "", astRoot);
		return buf.toString().replace(Node.SPLICE, "...");
	}

	private void printNode(StringBuilder buf, String indent, Node node) {
		buf.append(indent);
		node.astString(buf, true);
		printAttributes(buf, node);
		buf.append('\n');

		indent += "    ";
		for (Node child = node.getFirstChild(); child != null; child = child.getNext())
			printNode(buf, indent, child);
	}

	private void printAttributes(StringBuilder buf, Node node) {
		if (node instanceof Heading)
			printAttribute(buf, "level", ((Heading) node).getLevel());
	}

	private void printAttribute(StringBuilder buf, String name, Object value) {
		buf.append(' ').append(name).append(':').append(value);
	}

	//---- class MyAttributeProvider ------------------------------------------

	private static class MyAttributeProvider
		implements AttributeProvider {
		private static class Factory
			extends IndependentAttributeProviderFactory {

			@Override
			public @NotNull AttributeProvider apply(@NotNull LinkResolverContext context) {
				return new MyAttributeProvider();
			}
		}

		@Override
		public void setAttributes(Node node, AttributablePart part, Attributes attributes) {
			attributes.addValue("data-pos", node.getStartOffset() + ":" + node.getEndOffset());
		}
	}
}
