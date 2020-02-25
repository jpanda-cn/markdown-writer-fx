package org.markdownwriterfx.extensions.uml;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;
import org.markdownwriterfx.renderer.PlantUmlNodeRenderer;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/23 16:29
 */
public class PlantUmlExtension implements HtmlRenderer.HtmlRendererExtension {


	@Override
	public void rendererOptions(@NotNull MutableDataHolder options) {

	}

	@Override
	public void extend(HtmlRenderer.@NotNull Builder htmlRendererBuilder, @NotNull String rendererType) {
		htmlRendererBuilder.nodeRendererFactory(new PlantUmlNodeRenderer.Factory());
	}

	public static PlantUmlExtension create() {
		return new PlantUmlExtension();
	}
}
