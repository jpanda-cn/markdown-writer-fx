package org.markdownwriterfx.extensions.img.internal;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/26 9:18
 */
public class HttpProtocolConverter implements ProtocolConverter {

	private String customHttpProtocol;

	private String customHttpsProtocol;

	public static HttpProtocolConverter create() {
		return new HttpProtocolConverter();
	}

	@Override
	public void init(String customHttpProtocol, String customHttpsProtocol) {
		this.customHttpProtocol = customHttpProtocol;
		this.customHttpsProtocol = customHttpsProtocol;
	}

	@Override
	public EHttpType loadHttpProtocol(String key) {
		if (customHttpProtocol.equals(key)) {
			return EHttpType.HTTP;
		} else if (customHttpsProtocol.equals(key)) {
			return EHttpType.HTTPS;
		}
		return null;
	}

	@Override
	public String loadCustomProtocol(EHttpType httpType) {
		switch (httpType) {
			case HTTP: {
				return customHttpProtocol;
			}
			case HTTPS: {
				return customHttpsProtocol;
			}
			default: {
				return null;
			}
		}
	}

}
