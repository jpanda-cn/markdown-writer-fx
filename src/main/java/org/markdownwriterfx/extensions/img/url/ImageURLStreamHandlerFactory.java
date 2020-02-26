package org.markdownwriterfx.extensions.img.url;


import java.net.URL;
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

	private static volatile boolean isRegistry = false;

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


	public static void registrySelf() {
		if (isRegistry) {
			return;
		}
		synchronized (ImageURLStreamHandlerFactory.class) {
			URL.setURLStreamHandlerFactory(new ImageURLStreamHandlerFactory());
			isRegistry = true;
		}
	}

	public static void registrySelf(String httpKey, String httpsKey) {
		if (isRegistry) {
			return;
		}
		synchronized (ImageURLStreamHandlerFactory.class) {
			URL.setURLStreamHandlerFactory(new ImageURLStreamHandlerFactory(httpKey, httpsKey));
			isRegistry = true;
		}
	}
}
