package com.trade.algotrade.client.angelone.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "clientcode",
})
@Builder
@Data
public class LogoutUserRequest {
    @JsonProperty("clientcode")
    public String clientcode;
}