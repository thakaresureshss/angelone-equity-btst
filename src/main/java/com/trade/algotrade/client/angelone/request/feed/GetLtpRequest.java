package com.trade.algotrade.client.angelone.request.feed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "exchange",
        "tradingsymbol",
        "symboltoken"
})

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetLtpRequest {

    @JsonProperty("exchange")
    public String exchange;
    @JsonProperty("tradingsymbol")
    public String tradingsymbol;
    @JsonProperty("symboltoken")
    public String symboltoken;

}