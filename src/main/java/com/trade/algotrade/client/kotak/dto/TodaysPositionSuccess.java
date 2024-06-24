package com.trade.algotrade.client.kotak.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TodaysPositionSuccess {

	private Long actualNetTrdValue;
	private Long averageStockPrice;
	private Long buyOpenQtyLot;
	private Long buyOpenVal;
	private Long buyTradedQtyLot;
	private BigDecimal buyTradedVal;
	private BigDecimal buyTrdAvg;
	private Long deliveryStatus;
	private Long denominator;
	private String exchange;
	private String expiryDate;
	private Long exposureMargin;
	private Long exposureMarginTotal;
	private Long grossUtilization;
	private String instrumentName;
	private Long instrumentToken;
	private BigDecimal lastPrice;
	private String marginType;
	private Long marketLot;
	private Long maxCODQty;
	private Long maxSingleOrderQty;
	private Long maxSingleOrderValue;
	private Long multiplier;
	private BigDecimal netChange;
	private Long netTrdQtyLot;
	private Long netTrdValue;
	private Long normalSqOffQty;
	private String optionType;
	private BigDecimal percentChange;
	private BigDecimal premium;
	private String qtyUnit;
	private Long rbiRefRate;
	private BigDecimal realizedPL;
	private String segment;
	private Long sellOpenQtyLot;
	private Long sellOpenVal;
	private Long sellTradedQtyLot;
	private BigDecimal sellTradedVal;
	private BigDecimal sellTrdAvg;
	private Long spanMargin;
	private Long spanMarginTotal;
	private Long spreadTotal;
	private Long strikePrice;
	private String symbol;
	private Long totalStock;
}
