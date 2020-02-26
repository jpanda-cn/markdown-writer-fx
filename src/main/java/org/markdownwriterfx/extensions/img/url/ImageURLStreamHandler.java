package org.markdownwriterfx.extensions.img.url;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/25 15:15
 */
public class ImageURLStreamHandler extends URLStreamHandler {
	private boolean isHttps;
	public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36";

	private final String userAgent;

	public ImageURLStreamHandler(boolean isHttps) {
		this(isHttps, DEFAULT_USER_AGENT);
	}

	public ImageURLStreamHandler(boolean isHttps, String userAgent) {
		this.isHttps = isHttps;
		this.userAgent = userAgent;
	}

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		URL connectionUrl = new URL(isHttps ? "https" : "http", u.getHost(), u.getPort(), u.getFile().replaceAll("\\\\", "/"));
		HttpURLConnection httpURLConnection = (HttpURLConnection) connectionUrl.openConnection();
		httpURLConnection.setRequestMethod("GET");
		httpURLConnection.setRequestProperty("User-Agent", userAgent);
		return httpURLConnection;
	}

}
