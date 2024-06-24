package com.trade.algotrade.client.kotak.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SegmentsClosed {

	@JsonProperty("segment_name")
	private SegmentName segmentName;

	@JsonProperty("type")
	private Type type;

	public SegmentName getSegmentName() {
		return segmentName;
	}

	public void setSegmentName(SegmentName value) {
		this.segmentName = value;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type value) {
		this.type = value;
	}
}
