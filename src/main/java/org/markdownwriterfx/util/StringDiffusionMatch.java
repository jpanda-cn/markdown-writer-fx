package org.markdownwriterfx.util;

import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/29 14:56
 */
public class StringDiffusionMatch {
	private static Map<Character, Character> bracket = new HashMap<>();

	private static List<Character> singleSymbols = "`~!@#$^&*)=|}:;,].>/?~！@#￥……&*）——|}】；：”。，、？ \n".chars().mapToObj((c) -> (char) c).collect(Collectors.toList());

	private static Map<Character, Integer> pos = new HashMap<>();

	static {
		bracket.put(')', '(');
		bracket.put('）', '（');
		bracket.put(']', '[');
		bracket.put('>', '<');
		bracket.put('》', '《');
		bracket.put('】', '【');
		bracket.put('}', '{');
		bracket.put('`', '`');
		bracket.put('\'', '\'');
		bracket.put('"', '"');
		bracket.put('’', '‘');
		bracket.put('”', '“');
	}


	public Text loadNode(BasedSequence s) {

		boolean needSkip = false;
		char first = s.charAt(0);
		char end = s.charAt(s.length() - 1);
		if (bracket.containsKey(end) && bracket.get(end) == first) {
			needSkip = true;
		}
		// 获取完整文本对象
		Text text = new Text(s);

		// 处理文本，生成子节点，分段获取文本内容

		int start = 0;
		for (int i = 0; i < s.length(); i++) {

			Character temp = s.charAt(i);
			// 匹配到普通符号，且前面没有开始符号，将模式添加到节点内
			boolean symbol = singleSymbols.contains(temp);
			boolean matchStart = bracket.containsValue(temp) && (!needSkip || i != 0);
			if (!(symbol || matchStart)) {
				// 普通文本不需要处理
				continue;
			}


			// 无匹配开始符号，所有的符号都会分割文本，生成新的Text对象，并重置下一个对象的起始符号
			if (pos.size() == 0 && !matchStart) {
				Text matchText = new Text(text.getBaseSequence().subSequence(start, i));
				text.appendChild(matchText);
				start = i + 1;
				continue;
			}
			// ====  已经存在开始符号 或其本身为开始符号 ====

			// 判断是否匹配开始符号
			if (matchStart) {
				Text matchText = new Text(text.getBaseSequence().subSequence(start, i));
				text.appendChild(matchText);
				start = i + 1;
				pos.put(temp, i);

				continue;
			}


			// 处理已经存在开始符号的场景

			// 匹配结束符号
			boolean matchEnd = bracket.containsKey(temp)
				&& pos.containsKey(bracket.get(temp)) && (i == 0 || ((s.charAt(i - 1) != '\\') || (i == 1 || s.charAt(i - 2) == '\\')));

			// 匹配到结束符号
			if (matchEnd) {
				int subStart = pos.get(bracket.get(temp));
				//  递归处理匹配范围内的文本， 目前将会导致死循环，需要添加flag标志忽略掉首尾符号
				// 重置标记
				start = i + 1;
				text.appendChild(loadNode(text.getBaseSequence().subSequence(subStart, start)));

				continue;
			}

			// 不能匹配 普通文本或者结束符号或者普通符号在开始范围内,比如: {123. 3213 123.13

		}
		if (start != s.length() - 1) {
			text.appendChild(new Text(text.getBaseSequence().subSequence(start, s.length())));
		}
		return text;
	}
}
