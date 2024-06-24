
package com.trade.algotrade.client.angelone.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.trade.algotrade.client.angelone.response.BaseResponse;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data"
})
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Data
public class LogoutUserResponse extends BaseResponse {
    @JsonProperty("data")
    private LogoutData data;

}
