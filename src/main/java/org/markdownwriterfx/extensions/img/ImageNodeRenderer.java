package org.markdownwriterfx.extensions.img;

import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.ImageRef;
import com.vladsch.flexmark.ast.Reference;
import com.vladsch.flexmark.ast.util.ReferenceRepository;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.html.Attribute;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.sequence.Escaping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.markdownwriterfx.extensions.img.url.ImageURLStreamHandlerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.vladsch.flexmark.html.renderer.LinkStatus.UNKNOWN;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/25 16:14
 */
public class ImageNodeRenderer implements NodeRenderer {
	final private ReferenceRepository referenceRepository;
	final private boolean recheckUndefinedReferences;
	final private ImageOptions imageOptions;

	public ImageNodeRenderer(DataHolder options) {
		referenceRepository = Parser.REFERENCES.get(options);
		recheckUndefinedReferences = HtmlRenderer.RECHECK_UNDEFINED_REFERENCES.get(options);
		imageOptions = new ImageOptions(options);
		init();
	}

	protected void init() {
		ImageURLStreamHandlerFactory.registrySelf(imageOptions.customHttpProtocol, imageOptions.customHttpsProtocol);
	}

	@Override
	public @Nullable
	Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		return new HashSet<>(Arrays.asList(
			new NodeRenderingHandler<>(Image.class, this::render)
			, new NodeRenderingHandler<>(ImageRef.class, this::render)
		));
	}

	void render(Image node, NodeRendererContext context, HtmlWriter html) {
		if (!(context.isDoNotRenderLinks() || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context))) {
			String altText = new TextCollectingVisitor().collectAndGetText(node);
			ResolvedLink resolvedLink = context.resolveLink(LinkType.IMAGE, node.getUrl().unescape(), null, null);
			String url = resolvedLink.getUrl();

			if (!node.getUrlContent().isEmpty()) {
				// reverse URL encoding of =, &
				String content = Escaping.percentEncodeUrl(node.getUrlContent()).replace("+", "%2B").replace("%3D", "=").replace("%26", "&amp;");
				url += content;
			}

			html.attr("src", handlerUrl(url));
			html.attr("alt", altText);

			// we have a title part, use that
			if (node.getTitle().isNotNull()) {
				resolvedLink.getNonNullAttributes().replaceValue(Attribute.TITLE_ATTR, node.getTitle().unescape());
			} else {
				resolvedLink.getNonNullAttributes().remove(Attribute.TITLE_ATTR);
			}

			html.attr(resolvedLink.getAttributes());
			html.srcPos(node.getChars()).withAttr(resolvedLink).tagVoid("img");
		}
	}

	void render(ImageRef node, NodeRendererContext context, HtmlWriter html) {
		ResolvedLink resolvedLink;
		boolean isSuppressed = false;

		if (!node.isDefined() && recheckUndefinedReferences) {
			if (node.getReferenceNode(referenceRepository) != null) {
				node.setDefined(true);
			}
		}

		Reference reference = null;

		if (node.isDefined()) {
			reference = node.getReferenceNode(referenceRepository);
			String url = reference.getUrl().unescape();
			isSuppressed = CoreNodeRenderer.isSuppressedLinkPrefix(url, context);

			resolvedLink = context.resolveLink(LinkType.IMAGE, url, null, null);
			if (reference.getTitle().isNotNull()) {
				resolvedLink.getNonNullAttributes().replaceValue(Attribute.TITLE_ATTR, reference.getTitle().unescape());
			} else {
				resolvedLink.getNonNullAttributes().remove(Attribute.TITLE_ATTR);
			}
		} else {
			// see if have reference resolver and this is resolved
			String normalizeRef = referenceRepository.normalizeKey(node.getReference());
			resolvedLink = context.resolveLink(LinkType.IMAGE_REF, normalizeRef, null, null);
			if (resolvedLink.getStatus() == UNKNOWN || resolvedLink.getUrl().isEmpty()) {
				resolvedLink = null;
			}
		}

		if (resolvedLink == null) {
			// empty ref, we treat it as text
			html.text(node.getChars().unescape());
		} else {
			if (!(context.isDoNotRenderLinks() || isSuppressed)) {
				String altText = new TextCollectingVisitor().collectAndGetText(node);
				Attributes attributes = resolvedLink.getNonNullAttributes();

				html.attr("src", handlerUrl(resolvedLink.getUrl()));
				html.attr("alt", altText);

				// need to take attributes for reference definition, then overlay them with ours
				if (reference != null) {
					attributes = context.extendRenderingNodeAttributes(reference, AttributablePart.NODE, attributes);
				}

				html.attr(attributes);
				html.srcPos(node.getChars()).withAttr(resolvedLink).tagVoid("img");
			}
		}
	}

	public String handlerUrl(String url) {
		if (url.startsWith("http")) {
			return url.replaceFirst("http", imageOptions.customHttpProtocol);
		}
		return url;
	}

	public static class Factory implements NodeRendererFactory {
		@NotNull
		@Override
		public NodeRenderer apply(@NotNull DataHolder options) {
			return new ImageNodeRenderer(options);
		}
	}
}
