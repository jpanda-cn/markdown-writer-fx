package org.markdownwriterfx.preview;

import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;

/**
 * @author HanQi [Jpanda@aliyun.com]
 * @version 1.0
 * @since 2020/3/3 12:04
 */
public class PreviewSyncNotify {
	/**
	 * ????
	 */
	private String key;

	private Double scrollY;

	private Double totalScrollY;

	private NotifyType notifyType;

	private IndexRange range;

	private Double viewHeight;

	private Double rangeHeight;

	private Double rangeY;

	private Bounds rangeBounds;

	private double lineProportion;

	private double originalProportion;

	public double getOriginalProportion() {
		return originalProportion;
	}

	public double getLineProportion() {
		return lineProportion;
	}

	public void setLineProportion(double lineProportion) {
		this.lineProportion = lineProportion;
	}

	public void setOriginalProportion(double originalProportion) {
		this.originalProportion = originalProportion;
	}

	public Bounds getRangeBounds() {
		return rangeBounds;
	}

	public void setRangeBounds(Bounds rangeBounds) {
		this.rangeBounds = rangeBounds;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Double getScrollY() {
		return scrollY;
	}

	public void setScrollY(Double scrollY) {
		this.scrollY = scrollY;
	}

	public Double getTotalScrollY() {
		return totalScrollY;
	}

	public void setTotalScrollY(Double totalScrollY) {
		this.totalScrollY = totalScrollY;
	}

	public NotifyType getNotifyType() {
		return notifyType;
	}

	public void setNotifyType(NotifyType notifyType) {
		this.notifyType = notifyType;
	}

	public IndexRange getRange() {
		return range;
	}

	public void setRange(IndexRange range) {
		this.range = range;
	}

	public Double getViewHeight() {
		return viewHeight;
	}

	public void setViewHeight(Double viewHeight) {
		this.viewHeight = viewHeight;
	}

	public Double getRangeHeight() {
		return rangeHeight;
	}

	public void setRangeHeight(Double rangeHeight) {
		this.rangeHeight = rangeHeight;
	}

	public Double getRangeY() {
		return rangeY;
	}

	public void setRangeY(Double rangeY) {
		this.rangeY = rangeY;
	}

	public static enum NotifyType {
		CARE, SCROLL;
	}

}
