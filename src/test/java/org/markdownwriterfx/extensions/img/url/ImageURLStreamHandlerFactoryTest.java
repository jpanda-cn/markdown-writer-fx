package org.markdownwriterfx.extensions.img.url;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.file.Paths;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/25 15:34
 */
public class ImageURLStreamHandlerFactoryTest {

	@Test
	public void loadUrl() throws Exception {
		URL.setURLStreamHandlerFactory(new ImageURLStreamHandlerFactory());
		String url="imgs://gitee.com/topanda/learning-notes/raw/images/images\\java\\mybatis\\%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90/e384aab4-cb8c-4c1b-9a27-ae14c2495c5b.png";
		System.out.println(url.replaceAll("\\\\","/"));
		BufferedImage b = ImageIO.read(new URL(url.replaceAll("\\\\","/")));
		ImageIO.write(b, "png", Paths.get(System.getProperty("user.dir"), "a.png").toFile());

		
	}

}
