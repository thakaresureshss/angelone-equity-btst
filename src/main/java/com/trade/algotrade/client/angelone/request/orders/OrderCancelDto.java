package com.trade.algotrade.client.angelone.request.orders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "variety",
        "orderid"
})
public class OrderCancelDto {

    @JsonProperty("variety")
    public String variety;

    @JsonProperty("orderid")
    public String orderid;

}