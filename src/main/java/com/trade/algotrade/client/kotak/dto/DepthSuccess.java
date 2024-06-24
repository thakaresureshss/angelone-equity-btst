package com.trade.algotrade.client.kotak.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class DepthSuccess implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<Depth> depth;
	private String instrumentName;
	private Long instrumentToken;
	private String lastUpdatedTime;
	private String lastTradedTime;
	private BigDecimal lastPrice;
	private Long lastTradedQuantity;
	private Long totalBuyQuantity;
	private Long totalSellQuantity;
	private BigDecimal averageTradedPrice;
	private Long openInterest;
}