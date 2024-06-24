package com.trade.algotrade.client.angelone.response.feed;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderUpdateResponse {
    @JsonProperty("user-id")
    private String userId;
    @JsonProperty("status-code")
    private String StatusCode;
    @JsonAlias("order-status")
    private String orderStatus;
    @JsonProperty("error-message")
    private String errorMessage;
    OrderUpdateData orderData;
}
