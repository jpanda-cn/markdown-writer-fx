package org.markdownwriterfx.extensions.img.internal;

/**
 * ?????
 *
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/26 9:17
 */
public interface ProtocolConverter {

	void init(String customHttpProtocol, String customHttpsProtocol);
	EHttpType loadHttpProtocol(String key);

	String loadCustomProtocol(EHttpType httpType);
}
