package com.trade.algotrade.client.kotak.dto;

import lombok.Data;

@Data
public class WsAccessToken {
	private String token;
	private long expiry;
}