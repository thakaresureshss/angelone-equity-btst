package com.trade.algotrade.client.angelone.request.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderDetailDto {
    private String script;

    @JsonProperty("orderid")
    private Long orderId;

    @JsonProperty("uniqueorderid")
    private String uniqueOrderId;

    @JsonProperty("averageprice")
    private BigDecimal averagePrice;

    @JsonProperty("transactiontype")
    private String transactionType;

    @JsonProperty("variety")
    private String variety;

    @JsonProperty("expirydate")
    private String expiryDate;

    private String status;

    @JsonProperty("cancelsize")
    private Integer cancelSize;
    @JsonProperty("filledshares")
    private Integer filledShares;

    private Integer quantity;
    @JsonProperty("exchorderupdatetime")
    private String exchOrderUpdateTime;
}

