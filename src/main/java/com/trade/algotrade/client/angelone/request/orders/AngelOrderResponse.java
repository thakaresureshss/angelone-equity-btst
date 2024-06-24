package com.trade.algotrade.client.angelone.request.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class AngelOrderResponse {

    private String status;

    private String message;

    @JsonProperty("errorCode")
    private String errorCode;

    private OrderDetailDto data;
}
