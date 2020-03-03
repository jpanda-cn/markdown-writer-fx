/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.markdownwriterfx.editor;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.input.*;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Caret.CaretVisibility;
import org.fxmisc.richtext.CaretNode;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.Selection;
import org.fxmisc.richtext.SelectionImpl;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.wellbehaved.event.Nodes;
import org.markdownwriterfx.controls.BottomSlidePane;
import org.markdownwriterfx.editor.FindReplacePane.HitsChangeListener;
import org.markdownwriterfx.editor.MarkdownSyntaxHighlighter.ExtraStyledRanges;
import org.markdownwriterfx.options.MarkdownExtensions;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.preview.MarkdownPreviewPane;
import org.markdownwriterfx.preview.PreviewSyncNotify;
import org.markdownwriterfx.util.StringDiffusionMatch;
import org.reactfx.util.Either;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;

/**
 * Markdown editor pane.
 * <p>
 * Uses flexmark-java (https://github.com/vsch/flexmark-java) for parsing markdown.
 *
 * @author Karl Tauber
 */
public class MarkdownEditorPane {
	private final BottomSlidePane borderPane;
	private final MarkdownTextArea textArea;

	private final ParagraphOverlayGraphicFactory overlayGraphicFactory;
	private LineNumberGutterFactory lineNumberGutterFactory;
	private WhitespaceOverlayFactory whitespaceOverlayFactory;
	private ContextMenu contextMenu;
	private CaretNode dragCaret;
	private final SmartEdit smartEdit;

	private final FindReplacePane findReplacePane;
	private final HitsChangeListener findHitsChangeListener;
	private Parser parser;
	private final InvalidationListener optionsListener;
	private String lineSeparator = getLineSeparatorOrDefault();

	private Selection<Collection<String>, Either<String, EmbeddedImage>, Collection<String>> extraSelection;

	private MarkdownPreviewPane markdownPreviewPane;

	public void setMarkdownPreviewPane(MarkdownPreviewPane markdownPreviewPane) {
		this.markdownPreviewPane = markdownPreviewPane;
	}

	public MarkdownEditorPane() {
		textArea = new MarkdownTextArea();
		textArea.setWrapText(true);
		textArea.setUseInitialStyleForInsertion(true);
		textArea.getStyleClass().add("markdown-editor");
		textArea.getStylesheets().add("org/markdownwriterfx/editor/MarkdownEditor.css");
		textArea.getStylesheets().add("org/markdownwriterfx/prism.css");
		textArea.textProperty().addListener((observable, oldText, newText) -> {
			textChanged(newText);
		});

		textArea.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::showContextMenu);
		textArea.addEventHandler(MouseEvent.MOUSE_PRESSED, this::hideContextMenu);
		textArea.setOnDragEntered(this::onDragEntered);
		textArea.setOnDragExited(this::onDragExited);
		textArea.setOnDragOver(this::onDragOver);
		textArea.setOnDragDropped(this::onDragDropped);
		textArea.caretBoundsProperty().addListener((observable, oldValue, newValue) -> flushViewY());

		extraSelection = new SelectionImpl<>("", textArea, path -> {
			path.setVisible(false);
		});
		textArea.addSelection(extraSelection);

		smartEdit = new SmartEdit(this, textArea);

		Nodes.addInputMap(textArea, sequence(
			consume(keyPressed(PLUS, SHORTCUT_DOWN), this::increaseFontSize),
			consume(keyPressed(MINUS, SHORTCUT_DOWN), this::decreaseFontSize),
			consume(keyPressed(DIGIT0, SHORTCUT_DOWN), this::resetFontSize),
			consume(keyPressed(W, ALT_DOWN), this::showWhitespace),
			consume(keyPressed(I, ALT_DOWN), this::showImagesEmbedded),
			consume(keyPressed(W, CONTROL_DOWN), this::selectWords)
		));


		// create scroll pane
		VirtualizedScrollPane<MarkdownTextArea> scrollPane = new VirtualizedScrollPane<>(textArea);

		// create border pane
		borderPane = new BottomSlidePane(scrollPane);

		overlayGraphicFactory = new ParagraphOverlayGraphicFactory(textArea);
		textArea.setParagraphGraphicFactory(overlayGraphicFactory);
		updateFont();
		updateShowLineNo();
		updateShowWhitespace();

		// initialize properties
		markdownText.set("");
		markdownAST.set(parseMarkdown(""));

		// find/replace
		findReplacePane = new FindReplacePane(textArea);
		findHitsChangeListener = this::findHitsChanged;
		findReplacePane.addListener(findHitsChangeListener);
		findReplacePane.visibleProperty().addListener((ov, oldVisible, newVisible) -> {
			if (!newVisible)
				borderPane.setBottom(null);
		});

		// listen to option changes
		optionsListener = e -> {
			if (textArea.getScene() == null)
				return; // editor closed but not yet GCed

			if (e == Options.fontFamilyProperty() || e == Options.fontSizeProperty())
				updateFont();
			else if (e == Options.showLineNoProperty())
				updateShowLineNo();
			else if (e == Options.showWhitespaceProperty())
				updateShowWhitespace();
			else if (e == Options.showImagesEmbeddedProperty())
				updateShowImagesEmbedded();
			else if (e == Options.markdownRendererProperty() || e == Options.markdownExtensionsProperty()) {
				// re-process markdown if markdown extensions option changes
				parser = null;
				textChanged(textArea.getText());
			}
		};
		WeakInvalidationListener weakOptionsListener = new WeakInvalidationListener(optionsListener);
		Options.fontFamilyProperty().addListener(weakOptionsListener);
		Options.fontSizeProperty().addListener(weakOptionsListener);
		Options.markdownRendererProperty().addListener(weakOptionsListener);
		Options.markdownExtensionsProperty().addListener(weakOptionsListener);
		Options.showLineNoProperty().addListener(weakOptionsListener);
		Options.showWhitespaceProperty().addListener(weakOptionsListener);
		Options.showImagesEmbeddedProperty().addListener(weakOptionsListener);

		// workaround a problem with wrong selection after undo:
		//   after undo the selection is 0-0, anchor is 0, but caret position is correct
		//   --> set selection to caret position
		textArea.selectionProperty().addListener((observable, oldSelection, newSelection) -> {
			// use runLater because the wrong selection temporary occurs while edition
			Platform.runLater(() -> {
				IndexRange selection = textArea.getSelection();
				int caretPosition = textArea.getCaretPosition();
				if (selection.getStart() == 0 && selection.getEnd() == 0 && textArea.getAnchor() == 0 && caretPosition > 0)
					textArea.selectRange(caretPosition, caretPosition);
			});
		});

		scrollYProperty().addListener((observable, oldValue, newValue) -> flushPreview());

//		updateYProperty().addListener((observable, oldValue, newValue) -> flushPreview());
	}

	private void updateFont() {
		textArea.setStyle("-fx-font-family: '" + Options.getFontFamily()
			+ "'; -fx-font-size: " + Options.getFontSize());
	}

	public javafx.scene.Node getNode() {
		return borderPane;
	}

	public boolean isReadOnly() {
		return textArea.isDisable();
	}

	public void setReadOnly(boolean readOnly) {
		textArea.setDisable(readOnly);
	}

	public BooleanProperty readOnlyProperty() {
		return textArea.disableProperty();
	}

	public UndoManager<?> getUndoManager() {
		return textArea.getUndoManager();
	}

	public SmartEdit getSmartEdit() {
		return smartEdit;
	}

	public void requestFocus() {
		Platform.runLater(() -> {
			if (textArea.getScene() != null)
				textArea.requestFocus();
			else {
				// text area still does not have a scene
				// --> use listener on scene to make sure that text area receives focus
				ChangeListener<Scene> l = new ChangeListener<Scene>() {
					@Override
					public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
						textArea.sceneProperty().removeListener(this);
						textArea.requestFocus();
					}
				};
				textArea.sceneProperty().addListener(l);
			}
		});
	}

	private String getLineSeparatorOrDefault() {
		String lineSeparator = Options.getLineSeparator();
		return (lineSeparator != null) ? lineSeparator : System.getProperty("line.separator", "\n");
	}

	private String determineLineSeparator(String str) {
		int strLength = str.length();
		for (int i = 0; i < strLength; i++) {
			char ch = str.charAt(i);
			if (ch == '\n')
				return (i > 0 && str.charAt(i - 1) == '\r') ? "\r\n" : "\n";
		}
		return getLineSeparatorOrDefault();
	}

	// 'markdown' property
	public String getMarkdown() {
		String markdown = textArea.getText();
		if (!lineSeparator.equals("\n"))
			markdown = markdown.replace("\n", lineSeparator);
		return markdown;
	}

	public void setMarkdown(String markdown) {
		// remember old selection range
		IndexRange oldSelection = textArea.getSelection();

		// replace text
		lineSeparator = determineLineSeparator(markdown);
		textArea.replaceText(markdown);

		// restore old selection range
		int newLength = textArea.getLength();
		textArea.selectRange(Math.min(oldSelection.getStart(), newLength), Math.min(oldSelection.getEnd(), newLength));
	}

	public ObservableValue<String> markdownProperty() {
		return textArea.textProperty();
	}

	// 'markdownText' property
	private final ReadOnlyStringWrapper markdownText = new ReadOnlyStringWrapper();

	public String getMarkdownText() {
		return markdownText.get();
	}

	public ReadOnlyStringProperty markdownTextProperty() {
		return markdownText.getReadOnlyProperty();
	}

	// 'markdownAST' property
	private final ReadOnlyObjectWrapper<Node> markdownAST = new ReadOnlyObjectWrapper<>();

	public Node getMarkdownAST() {
		return markdownAST.get();
	}

	public ReadOnlyObjectProperty<Node> markdownASTProperty() {
		return markdownAST.getReadOnlyProperty();
	}

	// 'selection' property
	public ObservableValue<IndexRange> selectionProperty() {
		return textArea.selectionProperty();
	}

	// 'scrollY' property
	public double getScrollY() {
		return textArea.scrollY.getValue();
	}

	public ObservableValue<Double> scrollYProperty() {
		return textArea.scrollY;

	}

	public ObservableValue<Double> updateYProperty() {
		return textArea.updateY;
	}

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();

	public Path getPath() {
		return path.get();
	}

	public void setPath(Path path) {
		this.path.set(path);
	}

	public ObjectProperty<Path> pathProperty() {
		return path;
	}

	Path getParentPath() {
		Path path = getPath();
		return (path != null) ? path.getParent() : null;
	}

	private void textChanged(String newText) {
		if (borderPane.getBottom() != null) {
			findReplacePane.removeListener(findHitsChangeListener);
			findReplacePane.textChanged();
			findReplacePane.addListener(findHitsChangeListener);
		}

		if (isReadOnly())
			newText = "";

		Node astRoot = parseMarkdown(newText);

		if (Options.isShowImagesEmbedded())
			EmbeddedImage.replaceImageSegments(textArea, astRoot, getParentPath());

		applyHighlighting(astRoot);

		markdownText.set(newText);
		markdownAST.set(astRoot);
	}

	private void findHitsChanged() {
		applyHighlighting(markdownAST.get());
	}

	Node parseMarkdown(String text) {
		if (parser == null) {
			parser = Parser.builder()
				.extensions(MarkdownExtensions.getFlexmarkExtensions(Options.getMarkdownRenderer()))
				.build();
		}
		return parser.parse(text);
	}

	private void applyHighlighting(Node astRoot) {
		List<ExtraStyledRanges> extraStyledRanges = findReplacePane.hasHits()
			? Arrays.asList(
			new ExtraStyledRanges("hit", findReplacePane.getHits()),
			new ExtraStyledRanges("hit-active", Arrays.asList(findReplacePane.getActiveHit())))
			: null;

		MarkdownSyntaxHighlighter.highlight(textArea, astRoot, extraStyledRanges);
	}

	private void increaseFontSize(KeyEvent e) {
		Options.setFontSize(Options.getFontSize() + 1);
	}

	private void decreaseFontSize(KeyEvent e) {
		Options.setFontSize(Options.getFontSize() - 1);
	}

	private void resetFontSize(KeyEvent e) {
		Options.setFontSize(Options.DEF_FONT_SIZE);
	}

	private void showWhitespace(KeyEvent e) {
		Options.setShowWhitespace(!Options.isShowWhitespace());
	}

	private void showImagesEmbedded(KeyEvent e) {
		Options.setShowImagesEmbedded(!Options.isShowImagesEmbedded());
	}

	private void selectWords(KeyEvent e) {
		IndexRange diffuse = selectDiffuse(selectionProperty().getValue());
		selectRange(diffuse.getStart(), diffuse.getEnd());
	}

	private void updateShowLineNo() {
		boolean showLineNo = Options.isShowLineNo();
		if (showLineNo && lineNumberGutterFactory == null) {
			lineNumberGutterFactory = new LineNumberGutterFactory(textArea);
			overlayGraphicFactory.addGutterFactory(lineNumberGutterFactory);
		} else if (!showLineNo && lineNumberGutterFactory != null) {
			overlayGraphicFactory.removeGutterFactory(lineNumberGutterFactory);
			lineNumberGutterFactory = null;
		}
	}

	private void updateShowWhitespace() {
		boolean showWhitespace = Options.isShowWhitespace();
		if (showWhitespace && whitespaceOverlayFactory == null) {
			whitespaceOverlayFactory = new WhitespaceOverlayFactory();
			overlayGraphicFactory.addOverlayFactory(whitespaceOverlayFactory);
		} else if (!showWhitespace && whitespaceOverlayFactory != null) {
			overlayGraphicFactory.removeOverlayFactory(whitespaceOverlayFactory);
			whitespaceOverlayFactory = null;
		}
	}

	private void updateShowImagesEmbedded() {
		if (Options.isShowImagesEmbedded())
			EmbeddedImage.replaceImageSegments(textArea, getMarkdownAST(), getParentPath());
		else
			EmbeddedImage.removeAllImageSegments(textArea);
	}

	public void undo() {
		textArea.getUndoManager().undo();
	}

	public void redo() {
		textArea.getUndoManager().redo();
	}

	public void cut() {
		textArea.cut();
	}

	public void copy() {
		textArea.copy();
	}

	public void paste() {
		textArea.paste();
	}

	public void selectAll() {
		textArea.selectAll();
	}

	public void selectRange(int anchor, int caretPosition) {
		SmartEdit.selectRange(textArea, anchor, caretPosition);
	}

	//---- context menu -------------------------------------------------------

	private void showContextMenu(ContextMenuEvent e) {
		if (e.isConsumed())
			return;

		// create context menu
		if (contextMenu == null) {
			contextMenu = new ContextMenu();
			initContextMenu();
		}

		// update context menu
		CharacterHit hit = textArea.hit(e.getX(), e.getY());
		updateContextMenu(hit.getCharacterIndex().orElse(-1), hit.getInsertionIndex());

		if (contextMenu.getItems().isEmpty())
			return;

		// show context menu
		contextMenu.show(textArea, e.getScreenX(), e.getScreenY());
		e.consume();
	}

	private void hideContextMenu(MouseEvent e) {
		if (contextMenu != null)
			contextMenu.hide();
	}

	private void initContextMenu() {
		SmartEditActions.initContextMenu(this, contextMenu);
	}

	private void updateContextMenu(int characterIndex, int insertionIndex) {
		SmartEditActions.updateContextMenu(this, contextMenu, characterIndex);
	}

	//---- find/replace -------------------------------------------------------

	public void find(boolean replace) {
		if (borderPane.getBottom() == null)
			borderPane.setBottom(findReplacePane.getNode());

		findReplacePane.show(replace, true);
	}

	public void findNextPrevious(boolean next) {
		if (borderPane.getBottom() == null) {
			// show pane
			find(false);
			return;
		}

		if (next)
			findReplacePane.findNext();
		else
			findReplacePane.findPrevious();
	}

	//---- drag & drop --------------------------------------------------------

	private void onDragEntered(DragEvent event) {
		// create drag caret
		if (dragCaret == null) {
			dragCaret = new CaretNode("mwfx-drag-caret", textArea);
			dragCaret.getStyleClass().add("drag-caret");
			textArea.addCaret(dragCaret);
		}

		// show drag caret
		dragCaret.setShowCaret(CaretVisibility.ON);
	}

	private void onDragExited(DragEvent event) {
		// hide drag caret
		dragCaret.setShowCaret(CaretVisibility.OFF);
	}

	private void onDragOver(DragEvent event) {
		// check whether we can accept a drop
		Dragboard db = event.getDragboard();
		if (db.hasString() || db.hasFiles())
			event.acceptTransferModes(TransferMode.COPY);

		// move drag caret to mouse location
		if (event.isAccepted()) {
			CharacterHit hit = textArea.hit(event.getX(), event.getY());
			dragCaret.moveTo(hit.getInsertionIndex());
		}

		event.consume();
	}

	private void onDragDropped(DragEvent event) {
		Dragboard db = event.getDragboard();
		if (db.hasFiles()) {
			// drop files (e.g. from project file tree)
			List<File> files = db.getFiles();
			if (!files.isEmpty())
				smartEdit.insertLinkOrImage(dragCaret.getPosition(), files.get(0).toPath());
		} else if (db.hasString()) {
			// drop text
			String newText = db.getString();
			int insertPosition = dragCaret.getPosition();
			SmartEdit.insertText(textArea, insertPosition, newText);
			SmartEdit.selectRange(textArea, insertPosition, insertPosition + newText.length());
		}

		textArea.requestFocus();

		event.setDropCompleted(true);
		event.consume();
	}

	public MarkdownTextArea getTextArea() {
		return textArea;
	}

	public IndexRange selectDiffuse(IndexRange range) {
		Node finalNode = loadNode(getMarkdownAST(), range);
		return selectDiffuse(finalNode, range);
	}

	public IndexRange selectDiffuse(Node match, IndexRange range) {
		if (match == null) {
			return range;
		}
		if (match instanceof InlineLinkNode) {
			return getRange(range, (InlineLinkNode) match);
		}
		if (match instanceof Text) {
			return getRange(range, (Text) match);
		}
		return getRange(range, match);
	}

	public IndexRange getRange(IndexRange range, InlineLinkNode match) {
		BasedSequence url = match.getUrl();
		if (beIncludeNode(url, range) && range.getLength() != url.length()) {
			return getRange(range, new Text(url));
		}
		return getRange(range, (Node) match);
	}

	public IndexRange getRange(IndexRange range, Text match) {
		boolean isFullMatch = match.getTextLength() == range.getLength();
		if (isFullMatch) {
			Node mp = match.getParent();
			if (mp != null) {
				return new IndexRange(mp.getStartOffset(), mp.getEndOffset());
			}
			return range;
		}


		Node finalNode = loadNode(new StringDiffusionMatch().loadNode(new Text(match.getChars().toString()).getBaseSequence()), new IndexRange(range.getStart() - match.getStartOffset(), range.getEnd() - match.getStartOffset()));
		if (finalNode == null) {
			return range;
		}

		boolean isFull = finalNode.getTextLength() == range.getLength();
		Node p = finalNode.getParent();
		if (!isFull || p == null) {
			return new IndexRange(finalNode.getStartOffset() + match.getStartOffset(), finalNode.getEndOffset() + match.getStartOffset());
		}
		return new IndexRange(p.getStartOffset() + match.getStartOffset(), p.getEndOffset() + match.getStartOffset());

//		return selectDiffuse(loadNode(getMarkdownAST(), range), range);
//		Text nt = new TMatch().loadNode(match.getBaseSequence());
//		IndexRange r = new IndexRange(range.getStart() - match.getStartOffset(), range.getEnd() - match.getStartOffset());
//		r = new TextMatcher(match.getChars().toString()).diffuse(r);
//		return new IndexRange(r.getStart() + match.getStartOffset(), r.getEnd() + match.getStartOffset());
	}

	public IndexRange getRange(IndexRange range, Node match) {
		// is full
		boolean isFull = match.getTextLength() == range.getLength();
		Node p = match.getParent();
		if (!isFull || p == null) {
			return new IndexRange(match.getStartOffset(), match.getEndOffset());
		}
		return new IndexRange(p.getStartOffset(), p.getEndOffset());
	}

	public Node selectDiffuseNode(Node match, IndexRange range) {

		if (match == null) {
			return null;
		}
		// is full
		boolean isFull = match.getTextLength() == range.getLength();
		Node p = match.getParent();
		if (!isFull || p == null) {
			return match;
		}
		return p;
	}

	private Node loadNode(Node node, IndexRange range) {

		if (beIncludeNode(node, range)) {
			// include , find child
			for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
				if (beIncludeNode(child, range)) {
					return loadNode(child, range);
				}
				// not find in child ,so return parent
			}
			return node;
		}
		return null;
	}

	private void flushPreview() {
		// ??y?
		PreviewSyncNotify notify = new PreviewSyncNotify();
		notify.setNotifyType(PreviewSyncNotify.NotifyType.SCROLL);
		notify.setOriginalProportion(scrollYProperty().getValue());
		notify.setLineProportion(((double) textArea.getCurrentParagraph() + 1) / (double) textArea.getParagraphs().size() + 1 / (double) textArea.getParagraphs().size());
		previewSync.set(notify);
	}

	private void flushViewY() {
		// Get the element at the current cursor
		IndexRange range = new IndexRange(textArea.getCaretPosition(), textArea.getCaretPosition());
		Node n = loadNode(getMarkdownAST(), range);
		if (n == null) {
			return;
		}
		if (n instanceof Document) {
			//New content, no node yet
			n = loadNearNode(n, range);
		}
		if (n == null) {
			return;
		}
		// Gets the element that the cursor is sitting around. By default, it gets forward.
		// If there isn't one, it searches backwards. If it doesn't, it gets the parent element.
		n = loadHaveId(n);
		if (n == null) {
			return;
		}

		try {
			extraSelection.selectRange(n.getStartOffset(), n.getEndOffset());

			// ???????????

			// ??ID
			String id = String.format("_%s_%d", n.getNodeName(), n.hashCode());
			// ??????
			double offset = 0;
			Optional<Bounds> b = extraSelection.getSelectionBounds();
			if (!b.isPresent()) {
				return;
			}
			offset = textArea.sceneToLocal(b.get()).getMaxY();
			// ??
			PreviewSyncNotify notify = new PreviewSyncNotify();
			notify.setKey(id);
			notify.setNotifyType(PreviewSyncNotify.NotifyType.CARE);
			notify.setRange(new IndexRange(n.getStartOffset(), n.getEndOffset()));
			notify.setRangeHeight(b.get().getHeight());
			notify.setRangeY(b.get().getMinY());
			notify.setScrollY(textArea.getEstimatedScrollY());
			notify.setTotalScrollY(textArea.totalHeightEstimateProperty().getValue());
			notify.setRangeBounds(b.get());
			notify.setOriginalProportion(textArea.getEstimatedScrollY() / textArea.getTotalHeightEstimate());
			notify.setLineProportion(((double) textArea.getCurrentParagraph() + 1) / (double) textArea.getParagraphs().size() + 1 / (double) textArea.getParagraphs().size());

			previewSync.set(notify);
		} catch (Exception e) {
			extraSelection.deselect();
		}
	}

	public Node loadNearNode(Node node, IndexRange range) {
		for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
			// Current position gap
			if (child.getStartOffset() >= range.getEnd()) {
				// Add in first position
				return child;
			}
			// Sandwiched between or at the end
			boolean matchPre = child.getEndOffset() <= range.getStart();
			boolean matchNext = child.getNext() == null || child.getNext().getStartOffset() >= range.getEnd();
			if (matchPre && matchNext) {
				return child;
			}
			// not find in child ,so return parent
		}
		return node;
	}

	public Node loadHaveId(Node node) {
		if (hasIdNode(node)) {
			return node;
		}
		// The current element has no ID attribute
		for (Node pre = node.getFirstChild(); pre != null; pre = pre.getNext()) {
			if (hasIdNode(pre)) {
				return pre;
			}
		}
		// Get the element after the current child element
//		for (Node next = node.getNext(); next != null; next = next.getNext()) {
//			if (hasIdNode(next)) {
//				return next;
//			}
//		}
		// find in parent
		if (node.getParent() == null) {
			return node;
		}
		return loadHaveId(node.getParent());
	}

	public boolean hasIdNode(Node node) {
		return (node instanceof Heading
			|| node instanceof Image
			|| node instanceof Paragraph
			|| node instanceof Code
			|| node instanceof BulletList
			|| node instanceof BulletListItem
			|| node instanceof FencedCodeBlock
			|| node instanceof Link)
			;
	}

	private boolean beIncludeNode(Node node, IndexRange range) {

		return node.getStartOffset() <= range.getStart() && node.getEndOffset() >= range.getEnd();

	}

	private boolean beIncludeNode(BasedSequence seq, IndexRange range) {

		return seq.getStartOffset() <= range.getStart() && seq.getEndOffset() >= range.getEnd();

	}


	// 'previewSync' property
	private final ObjectProperty<PreviewSyncNotify> previewSync = new SimpleObjectProperty<>();

	public ObjectProperty<PreviewSyncNotify> previewSyncProperty() {
		return previewSync;
	}
}
