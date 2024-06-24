package com.trade.algotrade.client.kotak.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.WsAccessToken;

import lombok.Data;

@Data
public class WebsocketAccessTokenResponse {
	private String userId;
	private String status;
	private WsAccessToken result;

	@JsonProperty("fault")
	private Fault fault;
}