package com.trade.algotrade.client.kotak.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebsocketTokenRequest {
	private String authentication;
}