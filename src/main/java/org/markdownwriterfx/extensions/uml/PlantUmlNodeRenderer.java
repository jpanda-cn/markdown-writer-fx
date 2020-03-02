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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

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
	public static DataKey<Boolean> PLANT_UML_LOCAL_RENDER_FORMAT_SVG = new DataKey<>("PLANT_UML_LOCAL_RENDER_FORMAT_SVG", Boolean.FALSE);
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
		html
			.tag("p")
			.attr("src", handlerText(node.getContentChars().normalizeEOL()))
			.withAttr()
			.raw(toSvg(node.getContentChars().normalizeEOL()))
			.closeTag("p");
	}

	public String toSvg(String uml) {
		if (StringUtils.isBlank(uml)) {
			return "";
		}
		try {
			return SvgGeneratorService.getInstance().generateSvgFromPlantUml(uml, plantUmlLocalRenderFormatSvg);
		}catch (PlantumlRuntimeException e){
			return "";
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
