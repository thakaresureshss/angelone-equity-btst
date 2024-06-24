
package com.trade.algotrade.client.angelone.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "jwtToken",
    "refreshToken",
    "feedToken"
})
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LoginData {

    @JsonProperty("jwtToken")
    private String jwtToken;
    @JsonProperty("refreshToken")
    private String refreshToken;
    @JsonProperty("feedToken")
    private String feedToken;

}
