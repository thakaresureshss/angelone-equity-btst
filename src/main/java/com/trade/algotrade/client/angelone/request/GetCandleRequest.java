package com.trade.algotrade.client.angelone.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.trade.algotrade.client.angelone.enums.AngelOneExchange;
import com.trade.algotrade.client.angelone.enums.CandleTimeFrame;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "exchange",
        "symboltoken",
        "interval",
        "fromdate",
        "todate"
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCandleRequest {

    @JsonProperty("exchange")
    public String exchange;

    @JsonProperty("symboltoken")
    public String symboltoken;

    @JsonProperty("interval")
    public String interval;

    @JsonProperty("fromdate")
    public String fromdate;

    @JsonProperty("todate")
    public String todate;

}