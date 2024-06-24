
package com.trade.algotrade.client.angelone.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "net",
    "availablecash",
    "availableintradaypayin",
    "availablelimitmargin",
    "collateral",
    "m2munrealized",
    "m2mrealized",
    "utiliseddebits",
    "utilisedspan",
    "utilisedoptionpremium",
    "utilisedholdingsales",
    "utilisedexposure",
    "utilisedturnover",
    "utilisedpayout"
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarginData {

    @JsonProperty("net")
    public String net;
    @JsonProperty("availablecash")
    public String availablecash;
    @JsonProperty("availableintradaypayin")
    public String availableintradaypayin;
    @JsonProperty("availablelimitmargin")
    public String availablelimitmargin;
    @JsonProperty("collateral")
    public String collateral;
    @JsonProperty("m2munrealized")
    public String m2munrealized;
    @JsonProperty("m2mrealized")
    public String m2mrealized;
    @JsonProperty("utiliseddebits")
    public String utiliseddebits;
    @JsonProperty("utilisedspan")
    public String utilisedspan;
    @JsonProperty("utilisedoptionpremium")
    public String utilisedoptionpremium;
    @JsonProperty("utilisedholdingsales")
    public String utilisedholdingsales;
    @JsonProperty("utilisedexposure")
    public String utilisedexposure;
    @JsonProperty("utilisedturnover")
    public String utilisedturnover;
    @JsonProperty("utilisedpayout")
    public String utilisedpayout;

}
