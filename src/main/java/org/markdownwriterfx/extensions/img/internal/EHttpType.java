package org.markdownwriterfx.extensions.img.internal;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/26 9:16
 */
public enum EHttpType {
	HTTP("http"), HTTPS("https");
	private String protocol;

	EHttpType(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocol() {
		return protocol;
	}
}
