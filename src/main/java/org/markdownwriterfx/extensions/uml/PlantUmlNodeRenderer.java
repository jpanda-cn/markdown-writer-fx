package org.markdownwriterfx.renderer;

import com.credibledoc.plantuml.exception.PlantumlRuntimeException;
import com.credibledoc.plantuml.svggenerator.SvgGeneratorService;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.DiagramDescription;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.util.Base64;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Character.LINE_SEPARATOR;

/**
 * ?? Plant UML
 *
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/2/23 15:20
 */
public class PlantUmlNodeRenderer implements NodeRenderer {
	private String plantUmlUrl;
	private String plantUmlFlag;
	private Boolean plantUmlLocalRender;
	private Boolean plantUmlLocalRenderFormatSvg;

	public static DataKey<String> PLANT_UML_URL = new DataKey<>("PLANT_UML_URL", "https://g.gravizo.com/svg?");
	public static DataKey<String> PLANT_UML_FLAG = new DataKey<>("PLANT_UML_FLAG", "plant-uml");
	public static DataKey<Boolean> PLANT_UML_LOCAL_RENDER = new DataKey<>("PLANT_UML_LOCAL_RENDER", Boolean.TRUE);
	public static DataKey<Boolean> PLANT_UML_LOCAL_RENDER_FORMAT_SVG = new DataKey<>("PLANT_UML_LOCAL_RENDER_FORMAT_SVG", Boolean.TRUE);
	protected DataHolder options;

	public PlantUmlNodeRenderer(DataHolder options) {
		this.options = options;
		this.plantUmlUrl = PLANT_UML_URL.get(options);
		this.plantUmlFlag = PLANT_UML_FLAG.get(options);
		this.plantUmlLocalRender = PLANT_UML_LOCAL_RENDER.get(options);
		this.plantUmlLocalRenderFormatSvg = PLANT_UML_LOCAL_RENDER_FORMAT_SVG.get(options);
	}

	@Override
	public @Nullable Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		Set<NodeRenderingHandler<?>> set = new HashSet<>();
		set.add(new NodeRenderingHandler<>(FencedCodeBlock.class, PlantUmlNodeRenderer.this::render));
		return set;
	}

	private void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
		if (node.getInfo().equals(plantUmlFlag)) {

			if (plantUmlLocalRender) {
				renderLocal(node, context, html);
			} else {
				renderOnline(node, context, html);
			}

		} else {
			context.delegateRender();
		}
	}

	public void renderOnline(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
		html
			.tag("p")
			.attr("src", handlerText(node.getContentChars().normalizeEOL()))
			.withAttr().tag("img").line()
			.closeTag("img").line()
			.closeTag("p");
	}

	public void renderLocal(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
//		html
////			.attr("src", handlerText(node.getContentChars().normalizeEOL()))
////			.withAttr()
//			.attr("id", String.format("_%s_%d", node.getNodeName(), node.hashCode()))
//			.withAttr()
//			.tag("p")
//			.raw(toSvg(node.getContentChars().normalizeEOL()))
//			.closeTag("p")
//		;
		html
			.tag("p")
			.attr("src", toPng(node.getContentChars().normalizeEOL()))
			.withAttr().tag("img").line()
			.closeTag("img").line()
			.closeTag("p");
	}

	public String toSvg(String uml) {
		if (StringUtils.isBlank(uml)) {
			return "";
		}
		try {
//			System.setProperty("plantuml.include.path", Paths.get(System.getProperty("user.dir"), "plant-uml").toFile().getAbsolutePath());
			System.setProperty("GRAPHVIZ_DOT", "D:\\usr\\local\\graphviz\\bin\\dot.exe");
			String svg = SvgGeneratorService.getInstance().generateSvgFromPlantUml(uml, plantUmlLocalRenderFormatSvg);
			return svg.replaceAll("\r\n", "");


//			return SvgGeneratorService.getInstance().generateSvgFromPlantUml(uml, plantUmlLocalRenderFormatSvg);
		} catch (PlantumlRuntimeException e) {
			return "";
		}
	}


	public static String toPng(String plantUml) {
		System.setProperty("GRAPHVIZ_DOT", "D:\\usr\\local\\graphviz\\bin\\dot.exe");
		try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			if (!plantUml.trim().startsWith("@startuml")) {
				plantUml = "@startuml" + LINE_SEPARATOR + plantUml;
			}
			if (!plantUml.trim().endsWith("@enduml")) {
				plantUml = plantUml + LINE_SEPARATOR + "@enduml";
			}

			SourceStringReader reader = new SourceStringReader(plantUml);


			FileFormatOption fileFormatOption = new FileFormatOption(FileFormat.PNG);
			DiagramDescription diagramDescription = reader.outputImage(os, fileFormatOption);
			return String.format("data:image/png;base64,%s", Base64.encodeBytes(os.toByteArray()));
			// The XML is stored into svg
//			return new String(os.toByteArray(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new PlantumlRuntimeException("PlantUML: " + plantUml, e);
		}
	}

	protected String handlerText(String url) {
		return plantUmlUrl + url;
	}

	public static class Factory implements NodeRendererFactory {
		@NotNull
		@Override
		public NodeRenderer apply(@NotNull DataHolder options) {
			return new PlantUmlNodeRenderer(options);
		}
	}
}
