package com.trade.algotrade.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InstrumentResponse {
    private Long instrumentToken;
    private String instrumentName;
    private String name;
    private BigDecimal lastPrice;
    private String expiry;
    private Integer strike;
    private BigDecimal tickSize;
    private Integer lotSize;
    private String instrumentType;
    private String segment;
    private String exchange;
    private String multiplier;
    private String exchangeToken;
    private String optionType;
    private String tradingSymbol;
}
