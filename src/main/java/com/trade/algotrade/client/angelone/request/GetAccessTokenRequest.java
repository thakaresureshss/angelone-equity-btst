package com.trade.algotrade.client.angelone.request;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "clientcode",
        "password",
        "totp"
})

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAccessTokenRequest {

    @JsonProperty("clientcode")
    public String clientcode;
    @JsonProperty("password")
    public String password;
    @JsonProperty("totp")
    public String totp;

}