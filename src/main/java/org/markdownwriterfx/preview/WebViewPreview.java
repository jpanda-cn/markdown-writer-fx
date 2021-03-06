
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

package org.markdownwriterfx.preview;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.Visitor;
import javafx.concurrent.Worker.State;
import javafx.scene.control.IndexRange;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.preview.MarkdownPreviewPane.PreviewContext;
import org.markdownwriterfx.preview.MarkdownPreviewPane.Renderer;
import org.markdownwriterfx.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * WebView preview.
 *
 * @author Karl Tauber
 */
class WebViewPreview
	implements MarkdownPreviewPane.Preview {
	private static final HashMap<String, String> prismLangDependenciesMap = new HashMap<>();

	private WebView webView;
	private final ArrayList<Runnable> runWhenLoadedList = new ArrayList<>();
	private int lastScrollX;
	private int lastScrollY;
	private IndexRange lastEditorSelection;


	WebViewPreview() {
	}

	private void createNodes() {

		webView = new WebView();
		webView.setFocusTraversable(false);

		// disable WebView default drag and drop handler to allow dropping markdown files
		webView.setOnDragEntered(null);
		webView.setOnDragExited(null);
		webView.setOnDragOver(null);
		webView.setOnDragDropped(null);
		webView.setOnDragDetected(null);
		webView.setOnDragDone(null);

		webView.getEngine().getLoadWorker().stateProperty().addListener((ob, o, n) -> {
			if (n == State.SUCCEEDED && !runWhenLoadedList.isEmpty()) {
				ArrayList<Runnable> runnables = new ArrayList<>(runWhenLoadedList);
				runWhenLoadedList.clear();

				for (Runnable runnable : runnables)
					runnable.run();
			}
		});
	}

	private void runWhenLoaded(Runnable runnable) {
		if (webView.getEngine().getLoadWorker().isRunning())
			runWhenLoadedList.add(runnable);
		else
			runnable.run();
	}

	@Override
	public javafx.scene.Node getNode() {
		if (webView == null)
			createNodes();
		return webView;
	}

	@Override
	public void update(PreviewContext context, Renderer renderer) {
		if (!webView.getEngine().getLoadWorker().isRunning()) {
			// get window.scrollX and window.scrollY from web engine,
			// but only if no worker is running (in this case the result would be zero)
			Object scrollXobj = webView.getEngine().executeScript("window.scrollX");
			Object scrollYobj = webView.getEngine().executeScript("window.scrollY");
			lastScrollX = (scrollXobj instanceof Number) ? ((Number) scrollXobj).intValue() : 0;
			lastScrollY = (scrollYobj instanceof Number) ? ((Number) scrollYobj).intValue() : 0;
		}
		lastEditorSelection = context.getEditorSelection();


		Path path = context.getPath();
		String base = (path != null)
			? ("<base href=\"" + path.getParent().toUri().toString() + "\">\n")
			: "";
		String scrollScript = (lastScrollX > 0 || lastScrollY > 0)
			? ("  onload='window.scrollTo(" + lastScrollX + ", " + lastScrollY + ");'")
			: "";

		webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {

			if (newValue == State.SUCCEEDED) {
				Document document = webView.getEngine().getDocument();
				// 获取所有a标签
				NodeList aList = document.getElementsByTagName("a");
				for (int i = 0; i < aList.getLength(); i++) {
					org.w3c.dom.Node n = aList.item(i);
					org.w3c.dom.Node href = n.getAttributes().getNamedItem("href");
					if (href == null) {
						continue;
					}

					String target = href.getTextContent();
					if (StringUtils.isBlank(target)) {
						continue;
					}
					String targetUri = target.trim();

					boolean toSelf = targetUri.startsWith("#");
					if (!(n instanceof EventTarget)) {
						continue;
					}

					EventTarget node = (EventTarget) n;
					EventListener e2 = evt -> {
						try {
							if (toSelf) {
								PreviewSyncNotify previewSyncNotify=new PreviewSyncNotify();
								previewSyncNotify.setKey(targetUri.replaceFirst("#", ""));
								previewSyncNotify.setOriginalProportion(0);
								scrollY(null,previewSyncNotify);
							} else {
								Desktop.getDesktop().browse(new URI(targetUri));
							}
						} catch (URISyntaxException | IOException e) {
							e.printStackTrace();
						} finally {
							evt.preventDefault();
							evt.stopPropagation();
						}
					};

					node.addEventListener("click", e2, true);
				}
			}
		});

		webView.getEngine().loadContent(
			"<!DOCTYPE html>\n"
				+ "<html>\n"
				+ "<head>\n"
				+ "<link rel=\"stylesheet\" href=\"" + getClass().getResource("markdownpad-github.css") + "\">\n"
				+ "<style>\n"
				+ Utils.defaultIfEmpty(Options.getAdditionalCSS(), "") + "\n"
				+ ".mwfx-editor-selection {\n"
				+ "  border-right: 5px solid #f47806;\n"
				+ "  margin-right: -5px;\n"
				+ "  background-color: rgb(253, 247, 241);\n"
				+ "}\n"
				+ "</style>\n"
				+ "<script src=\"" + getClass().getResource("preview.js") + "\"></script>\n"
				+ "<script> function scrollToElement(elementId) {document.getElementById(elementId).scrollIntoView({ \n" +
				"                block: 'start', \n" +
				"                behavior: 'smooth', \n" +
				"                inline: 'start'\n" +
				"            });} </script>"
				+ prismSyntaxHighlighting(context.getMarkdownAST())
				+ base
				+ "</head>\n"
				+ "<body" + scrollScript + ">\n"
				+ renderer.getHtml(false)
				+ "<script>" + highlightNodesAt(lastEditorSelection) + "</script>\n"
				+ "</body>\n"
				+ "</html>");

	}


	@Override
	public void scrollY(PreviewContext context, double value) {
		runWhenLoaded(() -> {
			webView.getEngine().executeScript("preview.scrollTo(" + value + ");");
		});
	}

	@Override
	public void scrollY(PreviewContext context, PreviewSyncNotify value) {
		runWhenLoaded(() -> {
			// Determine if there is a selected element,
			// if there is a selected element,
			// it means that the calculation will be processed according to the relative position

			boolean hasSelectedElement = StringUtils.isNotBlank(value.getKey());


			if (!hasSelectedElement) {
				return;
			}

			// Load the Y coordinate of the currently selected element
			Number selectedElementHeight = 0;
			Integer selectedElementY = 0;

			// Handling cursor movement events
			JSObject document = (JSObject) webView.getEngine().executeScript("document");
			JSObject choose = (JSObject) document.call("getElementById", value.getKey());
			if (choose == null) {
				// No selected element, no need to calculate the position of the specified element
				return;
			} else {
				selectedElementHeight = (Number) ((JSObject) choose.call("getBoundingClientRect")).getMember("height");

				// Load the absolute coordinates of the selected element
				JSObject preview = (JSObject) webView.getEngine().executeScript("preview");
				JSObject posArray = (JSObject) preview.call("getAbsPosition", choose);
				selectedElementY = (Integer) posArray.getMember("0");

			}

			// Get window object, ready to handle offset
			JSObject window = (JSObject) webView.getEngine().executeScript("window");
			Integer windowHeight = (Integer) window.getMember("innerHeight");

			// Calculate the position that should be offset this time


			// ?????????????
			double scrollY = selectedElementY - (double) windowHeight / 2;
			if (selectedElementHeight.doubleValue() > (double) windowHeight / 2) {
				// The currently selected element cannot be displayed in the second half of the screen
				// ?????????????????
				scrollY = selectedElementY + windowHeight - selectedElementHeight.doubleValue();
			}
			scrollY += selectedElementHeight.doubleValue() * value.getOriginalProportion();
			// Center selected element
//			window.call("scrollTo", 0, scrollY);
			window.call("scrollSmoothTo", scrollY);
		});
	}


	@Override
	public void editorSelectionChanged(PreviewContext context, IndexRange range) {
		if (range.equals(lastEditorSelection))
			return;
		lastEditorSelection = range;

		runWhenLoaded(() -> {
			webView.getEngine().executeScript(highlightNodesAt(range));
		});
	}

	private String highlightNodesAt(IndexRange range) {
		return "preview.highlightNodesAt(" + range.getEnd() + ")";
	}

	private String prismSyntaxHighlighting(Node astRoot) {
		initPrismLangDependencies();

		// check whether markdown contains fenced code blocks and remember languages
		ArrayList<String> languages = new ArrayList<>();
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			@Override
			protected void processNode(@NotNull Node node, boolean withChildren, @NotNull BiConsumer<Node, Visitor<Node>> processor) {
				if (node instanceof FencedCodeBlock) {
					String language = ((FencedCodeBlock) node).getInfo().toString();
					if (language.contains(language))
						languages.add(language);

					// dependencies
					while ((language = prismLangDependenciesMap.get(language)) != null) {
						if (language.contains(language))
							languages.add(0, language); // dependencies must be loaded first
					}
				} else
					visitChildren(node);
			}

		};
		visitor.visit(astRoot);

		if (languages.isEmpty())
			return "";

		// build HTML (only load used languages)
		// Note: not using Prism Autoloader plugin because it lazy loads/highlights, which causes flicker
		//       during fast typing; it also does not work with "alias" languages (e.g. js, html, xml, svg, ...)
		StringBuilder buf = new StringBuilder();
		buf.append("<link rel=\"stylesheet\" href=\"").append(getClass().getResource("prism/prism.css")).append("\">\n");
		buf.append("<script src=\"").append(getClass().getResource("prism/prism-core.min.js")).append("\"></script>\n");
		for (String language : languages) {
			URL url = getClass().getResource("prism/components/prism-" + language + ".min.js");
			if (url != null)
				buf.append("<script src=\"").append(url).append("\"></script>\n");
		}
		return buf.toString();
	}

	/**
	 * load and parse prism/lang_dependencies.txt
	 */
	private static void initPrismLangDependencies() {
		if (!prismLangDependenciesMap.isEmpty())
			return;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
			WebViewPreview.class.getResourceAsStream("prism/lang_dependencies.txt")))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("{"))
					continue;

				line = line.replaceAll("\\[([^\\]]+)\\]", "[not supported]");
				line = trimDelim(line, "{", "}");
				for (String str : line.split(",")) {
					String[] parts = str.split(":");
					if (parts[1].startsWith("["))
						continue; // not supported

					String key = trimDelim(parts[0], "\"", "\"");
					String value = trimDelim(parts[1], "\"", "\"");
					prismLangDependenciesMap.put(key, value);
				}
			}
		} catch (IOException e) {
			// ignore
		}
	}

	private static String trimDelim(String str, String leadingDelim, String trailingDelim) {
		str = str.trim();
		if (!str.startsWith(leadingDelim) || !str.endsWith(trailingDelim))
			throw new IllegalArgumentException(str);
		return str.substring(leadingDelim.length(), str.length() - trailingDelim.length());
	}

	@Override
	public void close() {
		webView = null;
	}
}
