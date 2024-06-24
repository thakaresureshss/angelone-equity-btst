package com.trade.algotrade.client.kotak.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class HoldingPositionSuccess {
	private Long actualMTM;
	private Long actualPNL;
	private BigDecimal averageStockPrice;
	private Long bnstCredit;
	private Long buyOpenQtyLot;
	private Long buyOpenValue;
	private Long delStockPrice;
	private Long deliveryStatus;
	private Long denominator;
	private String exchange;
	private String expiryDate;
	private Long fnoBnstCredit;
	private Long fnoPremium;
	private Long hairCut;
	private Long holdStock;
	private String instrumentName;
	private Long instrumentToken;
	private BigDecimal lastPrice;
	private Long marketLot;
	private Long maxSingleOrderQty;
	private Long maxSingleOrderValue;
	private Long mfHairCut;
	private Long multiple;
	private Long multipleType;
	private Long multiplier;
	private BigDecimal netChange;
	private Long netTrdQtyLot;
	private Long netTrdValue;
	private Long openingStockValue;
	private String optionType;
	private BigDecimal percentChange;
	private Long premium;
	private Long previousMTMTrades;
	private String qtyUnit;
	private Long rbiRefRate;
	private Long realizedPL;
	private Long securityValuation;
	private Long securityValuationMTF;
	private String segment;
	private Long sellOpenQtyLot;
	private Long sellOpenValue;
	private Long stockBalance;
	private Long strikePrice;
	private String symbol;
}
