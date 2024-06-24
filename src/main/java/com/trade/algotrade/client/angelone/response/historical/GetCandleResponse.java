package com.trade.algotrade.client.angelone.response.historical;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.trade.algotrade.client.angelone.response.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data"
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCandleResponse extends BaseResponse {

   @JsonProperty("data")
    public List<List<Object>> data;

}