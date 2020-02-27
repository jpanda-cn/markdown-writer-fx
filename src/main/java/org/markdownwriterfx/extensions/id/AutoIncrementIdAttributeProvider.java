package org.markdownwriterfx.extensions.id;

import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.html.Attributes;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/26 14:30
 */
public class AutoIncrementIdAttributeProvider implements AttributeProvider {
	
	@Override
	public void setAttributes(@NotNull Node node, @NotNull AttributablePart part, @NotNull Attributes attributes) {
		attributes.addValue("id", String.format("%s@%d",node.getNodeName(),node.hashCode()));
	}

	public static class Factory extends IndependentAttributeProviderFactory {

		@Override
		public @NotNull AttributeProvider apply(@NotNull LinkResolverContext context) {
			return new AutoIncrementIdAttributeProvider();
		}
	}
}
