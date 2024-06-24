package com.trade.algotrade.client.kotak.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.response.APIToken;

import lombok.Data;

@Data
public class Success implements Serializable {
	private static final long serialVersionUID = 1L;
	private APIToken apiToken;
	private String clientCode;
	private String clientName;
	private String emailID;
	private String[] enabledProducts;
	private String[] enabledSegments;
	private String loginTime;
	private String phoneNo;
	private String sessionToken;
	private String stwtFlag;
	private String userID;
	@JsonProperty("oneTimeToken")
	private String oneTimeToken;
}