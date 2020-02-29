package org.markdownwriterfx.editor;

import javafx.beans.property.StringProperty;
import org.fxmisc.richtext.model.TwoDimensional;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/26 10:51
 */
public class TextSelectStatusUpdater {


	private StringProperty selectChars;

	public TextSelectStatusUpdater(StringProperty selectChars) {
		this.selectChars = selectChars;
	}

	public static TextSelectStatusUpdater of(StringProperty property) {
		return new TextSelectStatusUpdater(property);
	}

	public void apply(MarkdownTextArea area) {

		area.selectionProperty().addListener((ob, o, n) -> {
			int startLine = offsetToLine(area, n.getStart());
			int endLine = offsetToLine(area, n.getEnd());
			int countChars = n.getLength();

			StringBuilder content = new StringBuilder();
			if (countChars > 0) {
				content
					.append(String.format("%s  chars  ", countChars))
				;
				if (endLine != startLine) {
					content.append(String.format(",%s line breaks;  ", endLine - startLine+1));
				}
			}

			int effect;
			int line;
			int charAt;
			if (o == null) {
				effect = n.getStart();
				line = startLine;
			} else {
				if (n.getStart() < o.getStart()) {
					effect = n.getStart();
					line = startLine;
				} else {
					effect = n.getEnd();
					line = endLine;
				}
			}
			charAt = effect - lineToStartOffset(area, line);

			content.append(String.format("%d : %d ", line+1, charAt+1));
			selectChars.set(content.toString());

		});

	}

	/**
	 * Returns the start offset of the given line.
	 */
	private int lineToStartOffset(MarkdownTextArea area, int line) {
		return area.getAbsolutePosition(line, 0);
	}

	private int offsetToLine(MarkdownTextArea area, int offset) {
		return area.offsetToPosition(offset, TwoDimensional.Bias.Forward).getMajor();
	}
}
