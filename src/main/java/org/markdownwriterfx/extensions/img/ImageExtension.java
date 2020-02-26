package org.markdownwriterfx.extensions.img;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;
import org.markdownwriterfx.extensions.img.internal.HttpProtocolConverter;
import org.markdownwriterfx.extensions.img.internal.ImageConstants;
import org.markdownwriterfx.extensions.img.internal.ProtocolConverter;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/25 16:32
 */
public class ImageExtension implements HtmlRenderer.HtmlRendererExtension {
	final public static DataKey<String> CUSTOM_HTTP_PROTOCOL_NAME = new DataKey<>("CUSTOM_HTTP_PROTOCOL_NAME", ImageConstants.DEFAULT_CUSTOM_HTTP_PROTOCOL_NAME);

	final public static DataKey<String> CUSTOM_HTTPS_PROTOCOL_NAME = new DataKey<>("CUSTOM_HTTPS_PROTOCOL_NAME", ImageConstants.DEFAULT_CUSTOM_HTTPS_PROTOCOL_NAME);

	final public static DataKey<ProtocolConverter> PROTOCOL_CONVERTER = new DataKey<>("PROTOCOL_CONVERTER", HttpProtocolConverter.create());


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
