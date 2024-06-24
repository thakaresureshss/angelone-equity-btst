
package com.trade.algotrade.client.angelone.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "status",
    "message",
    "errorcode",
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse {

    @JsonProperty("status")
    public Boolean status;
    @JsonProperty("message")
    public String message;
    @JsonProperty("errorcode")
    public String errorcode;

}
