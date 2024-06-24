package com.trade.algotrade.client.angelone.response.feed;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteData {

    @JsonProperty("fetched")
    List<GetLtpData> fetched;
}
