package com.trade.algotrade.client.angelone.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "refreshToken",
})
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RenewAccessTokenRequest {
    @JsonProperty("refreshToken")
    public String refreshToken;
}