
package com.trade.algotrade.client.angelone.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "jwtToken",
    "refreshToken",
    "feedToken"
})
public class LoginDataTest {

    @JsonProperty("jwtToken")
    private String jwtToken;
    @JsonProperty("refreshToken")
    private String refreshToken;
    @JsonProperty("feedToken")
    private String feedToken;

    /**
     * No args constructor for use in serialization
     *
     */
    public LoginDataTest() {
    }

    /**
     *
     * @param feedToken
     * @param jwtToken
     * @param refreshToken
     */
    public LoginDataTest(String jwtToken, String refreshToken, String feedToken) {
        super();
        this.jwtToken = jwtToken;
        this.refreshToken = refreshToken;
        this.feedToken = feedToken;
    }

    @JsonProperty("jwtToken")
    public String getJwtToken() {
        return jwtToken;
    }

    @JsonProperty("jwtToken")
    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    @JsonProperty("refreshToken")
    public String getRefreshToken() {
        return refreshToken;
    }

    @JsonProperty("refreshToken")
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @JsonProperty("feedToken")
    public String getFeedToken() {
        return feedToken;
    }

    @JsonProperty("feedToken")
    public void setFeedToken(String feedToken) {
        this.feedToken = feedToken;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(LoginDataTest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("jwtToken");
        sb.append('=');
        sb.append(((this.jwtToken == null)?"<null>":this.jwtToken));
        sb.append(',');
        sb.append("refreshToken");
        sb.append('=');
        sb.append(((this.refreshToken == null)?"<null>":this.refreshToken));
        sb.append(',');
        sb.append("feedToken");
        sb.append('=');
        sb.append(((this.feedToken == null)?"<null>":this.feedToken));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.jwtToken == null)? 0 :this.jwtToken.hashCode()));
        result = ((result* 31)+((this.feedToken == null)? 0 :this.feedToken.hashCode()));
        result = ((result* 31)+((this.refreshToken == null)? 0 :this.refreshToken.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof LoginDataTest) == false) {
            return false;
        }
        LoginDataTest rhs = ((LoginDataTest) other);
        return ((((this.jwtToken == rhs.jwtToken)||((this.jwtToken!= null)&&this.jwtToken.equals(rhs.jwtToken)))&&((this.feedToken == rhs.feedToken)||((this.feedToken!= null)&&this.feedToken.equals(rhs.feedToken))))&&((this.refreshToken == rhs.refreshToken)||((this.refreshToken!= null)&&this.refreshToken.equals(rhs.refreshToken))));
    }

}
