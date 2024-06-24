package com.trade.algotrade.client.kotak.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OpenPositionsSuccess {
    private BigDecimal actualMTM;
    private BigDecimal actualPNL;
    private BigDecimal averageStockPrice;
    private BigDecimal bnstCredit;
    private BigDecimal buyOpenQtyLot;
    private BigDecimal buyOpenValue;
    private BigDecimal delStockPrice;
    private BigDecimal deliveryStatus;
    private BigDecimal denominator;
    private String exchange;
    private String expiryDate;
    private BigDecimal exposureMargin;
    private BigDecimal exposureMarginTotal;
    private BigDecimal fnoBnstCredit;
    private BigDecimal fnoPremium;
    private BigDecimal grossUtilization;
    private BigDecimal hairCut;
    private BigDecimal holdStock;
    private String instrumentName;
    private Long instrumentToken;
    private BigDecimal lastPrice;
    private String marginType;
    private BigDecimal marketLot;
    private BigDecimal maxCODQty;
    private BigDecimal maxSingleOrderQty;
    private BigDecimal maxSingleOrderValue;
    private BigDecimal multiple;
    private BigDecimal multipleType;
    private BigDecimal multiplier;
    private BigDecimal netChange;
    private Integer netTrdQtyLot;
    private BigDecimal netTrdValue;
    private BigDecimal normalSqOffQty;
    private BigDecimal openingStockValue;
    private String optionType;
    private BigDecimal percentChange;
    private BigDecimal premium;
    private BigDecimal previousMTMTrades;
    private String qtyUnit;
    private BigDecimal rbiRefRate;
    private BigDecimal realizedPL;
    private String segment;
    private BigDecimal sellOpenQtyLot;
    private BigDecimal sellOpenValue;
    private BigDecimal spanMargin;
    private BigDecimal spanMarginTotal;
    private BigDecimal spreadTotal;
    private BigDecimal stockBalance;
    private BigDecimal strikePrice;
    private String symbol;
}