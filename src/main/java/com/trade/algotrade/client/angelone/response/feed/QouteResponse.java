package com.trade.algotrade.client.angelone.response.feed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.trade.algotrade.client.angelone.response.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data"
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QouteResponse extends BaseResponse {

    @JsonProperty("data")
    public QuoteData data;
}
