package com.trade.algotrade.client.kotak.dto;

import lombok.Data;

@Data
public class MarginDto {
	private String exchange;
	MarginCoreDto options;
	MarginCoreDto equity;
	// MarginCoreDto futures;
}