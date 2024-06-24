package com.trade.algotrade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderConfigDto {

    private String userId;
    private Integer tradeQuantity;
    private Integer noOfTradePerDay;
    private BigDecimal maxLossPerDay;
    private BigDecimal maxProfitPerDay;
}