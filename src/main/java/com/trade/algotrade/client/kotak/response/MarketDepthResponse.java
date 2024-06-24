package com.trade.algotrade.client.kotak.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.DepthSuccess;
import com.trade.algotrade.client.kotak.dto.Fault;

import lombok.Data;

@Data
public class MarketDepthResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	@JsonProperty("success")
	private DepthSuccess success;
	
	@JsonProperty("fault")
	private Fault fault;
}
