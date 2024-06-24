package com.trade.algotrade.client.angelone.request.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AngelAllOrdersResponse {

    private String status;

    private String message;

    @JsonProperty("errorCode")
    private String errorCode;

    private List<OrderDetailDto> data;
}
