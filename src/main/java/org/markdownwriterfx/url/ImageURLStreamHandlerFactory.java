package org.markdownwriterfx.url;


import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/25 15:15
 */
public class ImageURLStreamHandlerFactory implements URLStreamHandlerFactory {
	public static final String DEFAULT_HTTP_KEY = "img";
	public static final String DEFAULT_HTTPS_KEY = "imgs";
	private String httpKey;
	private String httpsKey;

	public ImageURLStreamHandlerFactory() {
		this(DEFAULT_HTTP_KEY, DEFAULT_HTTPS_KEY);
	}


	public ImageURLStreamHandlerFactory(String httpKey, String httpsKey) {
		this.httpKey = httpKey;
		this.httpsKey = httpsKey;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if (protocol.equalsIgnoreCase(httpKey)) {
			return new ImageURLStreamHandler(false);
		} else if (protocol.equalsIgnoreCase(httpsKey)) {
			return new ImageURLStreamHandler(true);
		}
		return null;
	}
}
