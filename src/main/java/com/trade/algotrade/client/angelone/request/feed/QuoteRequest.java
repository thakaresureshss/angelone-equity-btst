package com.trade.algotrade.client.angelone.request.feed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.trade.algotrade.client.angelone.enums.ExchangeType;
import com.trade.algotrade.client.angelone.enums.QuoteMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "mode",
        "exchangeTokens",
})

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class QuoteRequest implements Serializable {
    @JsonProperty("mode")
    String mode;
    @JsonProperty("exchangeTokens")
    public Map<String, List<String>> exchangeTokens;
}