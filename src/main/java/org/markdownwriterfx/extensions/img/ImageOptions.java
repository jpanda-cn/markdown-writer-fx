package org.markdownwriterfx.extensions.img;

import com.vladsch.flexmark.util.data.DataHolder;
import org.markdownwriterfx.extensions.img.internal.ProtocolConverter;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/26 9:14
 */
public class ImageOptions {
	final public String customHttpProtocol;
	final public String customHttpsProtocol;
	final public ProtocolConverter protocolConverter;

	public ImageOptions(DataHolder options) {
		this.customHttpProtocol = ImageExtension.CUSTOM_HTTP_PROTOCOL_NAME.get(options);
		this.customHttpsProtocol = ImageExtension.CUSTOM_HTTPS_PROTOCOL_NAME.get(options);
		this.protocolConverter = ImageExtension.PROTOCOL_CONVERTER.get(options);
		protocolConverter.init(customHttpProtocol, customHttpsProtocol);
	}
}
