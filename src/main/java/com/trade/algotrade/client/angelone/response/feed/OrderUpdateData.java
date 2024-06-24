package com.trade.algotrade.client.angelone.response.feed;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderUpdateData {
    private BigDecimal price;
    private String quantity;
    private String status;
    @JsonProperty("orderstatus")
    private String orderStatus;
    @JsonProperty("orderid")
    private String orderId;
    @JsonProperty("tradingsymbol")
    private String tradingSymbol;
    @JsonProperty("transactiontype")
    private String transactionType;
    private String variety;
    private String ordertype;
    private String ordertag;
    private String producttype;
    private BigDecimal triggerprice;
    private Integer disclosedquantity;
    private String duration;
    private BigDecimal squareoff;
    private BigDecimal stoploss;
    private BigDecimal trailingstoploss;
    private BigDecimal averageprice;
    private String exchange ;
    private String instrumenttype ;
    private Long symboltoken ;
    private Long strikeprice;
    private String optiontype;
    private String text;
    private String updatetime;
    private String exchtime;
    private String exchorderupdatetime;
    private String fillid;
    private String filltime;
    private String expirydate;
    private String parentorderid;
    private Integer lotsize;
    private Integer cancelsize;
    private Integer filledshares;
    private Integer unfilledshares;


}
