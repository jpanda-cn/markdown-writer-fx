package org.markdownwriterfx.extensions.img;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/25 16:32
 */
public class ImageExtension implements HtmlRenderer.HtmlRendererExtension {
	@Override
	public void rendererOptions(@NotNull MutableDataHolder options) {

	}

	@Override
	public void extend(HtmlRenderer.@NotNull Builder htmlRendererBuilder, @NotNull String rendererType) {
		htmlRendererBuilder.nodeRendererFactory(new ImageNodeRenderer.Factory());
	}

	public static ImageExtension create() {
		return new ImageExtension();
	}
}
