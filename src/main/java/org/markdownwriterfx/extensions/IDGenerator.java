package org.markdownwriterfx.extensions;

import com.vladsch.flexmark.util.ast.Node;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/3/3 14:09
 */
public class IDGenerator {

	private static IDGenerator idGenerator = new IDGenerator();

	public static IDGenerator getInstance() {
		return idGenerator;
	}

	public String generatorId(Node node) {
		return String.format("_%s_%d", node.getNodeName(), node.hashCode());
	}
}
