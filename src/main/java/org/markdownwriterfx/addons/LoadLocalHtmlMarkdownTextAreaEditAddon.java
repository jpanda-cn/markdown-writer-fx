package org.markdownwriterfx.addons;

import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.fxmisc.richtext.model.StyledDocument;
import org.markdownwriterfx.editor.MarkdownEditorPane;

import java.util.ArrayList;
import java.util.List;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/26 23:33
 */
public class LoadLocalHtmlMarkdownTextAreaEditAddon implements MarkdownTextAreaEditAddon {


	private ObservableList<String> removeHtmlProperty = FXCollections.observableArrayList();
	private ObservableList<String> waitAddHtmlProperty = FXCollections.observableArrayList();
	private SimpleStringProperty preDomProperty = new SimpleStringProperty();
	private SimpleStringProperty nextDomProperty = new SimpleStringProperty();

	@Override
	public void pre(int start, int end, StyledDocument replacement, MarkdownEditorPane previewPane) {
		List<Node> p = this.getParagraph(start, end, previewPane.getMarkdownAST(), new ArrayList<>());
		removeHtmlProperty.clear();
		preDomProperty.set(null);
		nextDomProperty.set(null);
		if (!p.isEmpty()) {
			Node pre = p.get(0).getPrevious();
			if (pre != null) {
				preDomProperty.set(getId(pre));
			}
			Node next = p.get(p.size() - 1).getNext();
			if (next != null) {
				nextDomProperty.set(getId(next));
			}
			for (Node n : p) {
				removeHtmlProperty.add(getId(n));
			}
		}
	}

	private String getId(Node n) {
		return String.format("%s@%d", n.getNodeName(), n.hashCode());
	}

	@Override
	public void post(int start, int end, StyledDocument replacement, MarkdownEditorPane previewPane) {
		List<Node> p2 = this.getParagraph(start, start + replacement.length(), previewPane.getMarkdownAST(), new ArrayList<>());
		waitAddHtmlProperty.clear();
		if (!p2.isEmpty()) {
			for (Node n : p2) {
				if (previewPane.getMarkdownPreviewPane().getActiveRenderer() != null) {
					waitAddHtmlProperty.add(previewPane.getMarkdownPreviewPane().getActiveRenderer().getHtml(n));
				}
			}
		}
	}

	private boolean isInNode(int start, int end, Node node) {
//		if (end == start) {
//			end++;
//		}
		return (start <= node.getStartOffset() && end >= node.getStartOffset())
			||
			(end >= node.getStartOffset() && end <= node.getEndOffset());
	}

	private List<Node> getParagraph(int start, int end, Node root, List<Node> nodes) {

		if (isInNode(start, end, root) && root instanceof Block) {
			// find in child
			List<Node> seqNodes = new ArrayList<>();
			for (Node child = root.getFirstChild(); child != null; child = child.getNext()) {
				if (isInNode(start, end, child) && child instanceof Block) {
					seqNodes.add(child);
				}
			}
			if (seqNodes.size() == 1) {
				// split
				return getParagraph(start, end, seqNodes.get(0), seqNodes);
			} else if (seqNodes.size() > 0) {
				// not split
				return seqNodes;
			}
			if (!(root instanceof Document)&&!nodes.contains(root)) {
				nodes.add(root);
			}
		}
		return nodes;
	}

	public ObservableList<String> getRemoveHtmlProperty() {
		return removeHtmlProperty;
	}

	public ObservableList<String> getWaitAddHtmlProperty() {
		return waitAddHtmlProperty;
	}

	public String getPreDomProperty() {
		return preDomProperty.get();
	}

	public SimpleStringProperty preDomPropertyProperty() {
		return preDomProperty;
	}

	public String getNextDomProperty() {
		return nextDomProperty.get();
	}

	public SimpleStringProperty nextDomPropertyProperty() {
		return nextDomProperty;
	}
}
