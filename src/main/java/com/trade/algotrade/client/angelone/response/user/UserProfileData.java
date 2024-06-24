
package com.trade.algotrade.client.angelone.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "clientcode",
    "name",
    "email",
    "mobileno",
    "exchanges",
    "products",
    "lastlogintime",
    "broker"
})
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserProfileData {

    @JsonProperty("clientcode")
    public String clientcode;
    @JsonProperty("name")
    public String name;
    @JsonProperty("email")
    public String email;
    @JsonProperty("mobileno")
    public String mobileno;
    @JsonProperty("exchanges")
    public List<String> exchanges;
    @JsonProperty("products")
    public List<String> products;
    @JsonProperty("lastlogintime")
    public String lastlogintime;
    @JsonProperty("broker")
    public String broker;

}
