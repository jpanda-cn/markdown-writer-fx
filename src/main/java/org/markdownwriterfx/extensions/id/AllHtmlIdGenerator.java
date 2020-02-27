package org.markdownwriterfx.extensions.id;

import com.vladsch.flexmark.ast.AnchorRefTarget;
import com.vladsch.flexmark.ast.util.AnchorRefTargetBlockVisitor;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.renderer.HeaderIdGeneratorFactory;
import com.vladsch.flexmark.html.renderer.HtmlIdGenerator;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/26 13:58
 */
public class AllHtmlIdGenerator implements HtmlIdGenerator {
	@Override
	public void generateIds(Document document) {
		final HashMap<String, Integer> headerBaseIds = new HashMap<>();
		final boolean resolveDupes = HtmlRenderer.HEADER_ID_GENERATOR_RESOLVE_DUPES.getFrom(document);
		final String toDashChars = HtmlRenderer.HEADER_ID_GENERATOR_TO_DASH_CHARS.getFrom(document);

		new AnchorRefTargetBlockVisitor() {
			@Override
			protected void visit(AnchorRefTarget node) {
				String text = node.getAnchorRefText();

				if (!text.isEmpty()) {
					String baseRefId = generateId(text, toDashChars);

					if (resolveDupes) {
						if (headerBaseIds.containsKey(baseRefId)) {
							int index = headerBaseIds.get(baseRefId);

							index++;
							headerBaseIds.put(baseRefId, index);
							baseRefId += "-" + index;
						} else {
							headerBaseIds.put(baseRefId, 0);
						}
					}

					node.setAnchorRefId(baseRefId);
				}
			}
		}.visit(document);
	}

	@Override
	public String getId(Node node) {
		return node instanceof AnchorRefTarget ? ((AnchorRefTarget) node).getAnchorRefId() : null;
	}

	@Override
	public @Nullable String getId(@NotNull CharSequence text) {
		return null;
	}

	@SuppressWarnings("WeakerAccess")
	public static String generateId(CharSequence headerText, String toDashChars) {
		int iMax = headerText.length();
		StringBuilder baseRefId = new StringBuilder(iMax);
		if (toDashChars == null) toDashChars = " -_";

		for (int i = 0; i < iMax; i++) {
			char c = headerText.charAt(i);
			if (Character.isAlphabetic(c)) baseRefId.append(Character.toLowerCase(c));
			else if (Character.isDigit(c)) baseRefId.append(c);
			else if (toDashChars.indexOf(c) != -1) baseRefId.append('-');
		}
		return baseRefId.toString();
	}

	public static class Factory implements HeaderIdGeneratorFactory {


		@Override
		public @NotNull HtmlIdGenerator create(@NotNull LinkResolverContext context) {
			return new AllHtmlIdGenerator();
		}

		@Override
		public @NotNull HtmlIdGenerator create() {
			return new AllHtmlIdGenerator();
		}
	}
}
