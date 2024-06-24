package com.trade.algotrade.client.kotak.dto;

import lombok.Data;

@Data
public class Depth {
	private Buy[] buy;
	private Buy[] sell;
	private String stkStrikePrice;
	private String upperCktLimit;
	private String lowerCktLimit;
}