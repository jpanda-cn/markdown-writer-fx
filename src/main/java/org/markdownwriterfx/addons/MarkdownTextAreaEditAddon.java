package org.markdownwriterfx.addons;

import org.fxmisc.richtext.model.StyledDocument;
import org.markdownwriterfx.editor.MarkdownEditorPane;

/**
 * ?
 *
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/26 23:20
 */
public interface MarkdownTextAreaEditAddon {
	void pre(int start, int end, StyledDocument replacement, MarkdownEditorPane previewPane);

	void post(int start, int end, StyledDocument replacement, MarkdownEditorPane previewPane);
}
