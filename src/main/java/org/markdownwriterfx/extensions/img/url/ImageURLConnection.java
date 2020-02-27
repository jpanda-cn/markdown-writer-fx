package org.markdownwriterfx.extensions.img.url;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/27 16:32
 */
public class ImageURLConnection extends URLConnection {

	private HttpURLConnection httpURLConnection;

	public ImageURLConnection(URL url, HttpURLConnection httpURLConnection, boolean enableCache) {
		super(url);
		this.httpURLConnection = httpURLConnection;
	}


	@Override
	public void connect() throws IOException {
		httpURLConnection.connect();

	}

	@Override
	public InputStream getInputStream() throws IOException {

		return httpURLConnection.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return httpURLConnection.getOutputStream();
	}
}
