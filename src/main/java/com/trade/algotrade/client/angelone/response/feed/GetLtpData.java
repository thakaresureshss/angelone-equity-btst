package com.trade.algotrade.client.angelone.response.feed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "exchange",
        "tradingsymbol",
        "symboltoken",
        "open",
        "high",
        "low",
        "close",
        "ltp",
        "percentChange",
        "lowerCircuit",
        "upperCircuit",
        "yearHigh",
        "yearLow"
})

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class    GetLtpData {

    @JsonProperty("exchange")
    public String exchange;

    @JsonProperty("tradingSymbol")
    public String tradingSymbol;

    @JsonProperty("symbolToken")
    public String symbolToken;

    @JsonProperty("open")
    public BigDecimal open;

    @JsonProperty("high")
    public BigDecimal high;

    @JsonProperty("low")
    public BigDecimal low;

    @JsonProperty("close")
    public BigDecimal close;

    @JsonProperty("ltp")
    public BigDecimal ltp;

    @JsonProperty("percentChange")
    public BigDecimal percentChange;

    @JsonProperty("lowerCircuit")
    public BigDecimal lowerCircuit;

    @JsonProperty("upperCircuit")
    public BigDecimal upperCircuit;

    @JsonProperty("yearHigh")
    public BigDecimal yearHigh;

    @JsonProperty("yearLow")
    public BigDecimal yearLow;


}