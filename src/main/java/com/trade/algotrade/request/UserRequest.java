package com.trade.algotrade.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {
	private Long id;
	private String userId;
	private String password;
	private String accessToken;
	private String broker;
	private String appId;
	private String consumerKey;
	private String consumerSecrete;
	private String exchange;
	private String oneTimeToken;
	private String sessionToken;
	private String websocketToken;
	private Map<String, BigDecimal> dayProfitLimit;
	private Map<String, BigDecimal> dayLossLimit;
	private Map<String, Integer> minTradesPerDay;
	private Map<String, Integer> maxTradesPerDay;
	private Boolean isRealTradingEnabled;
	private Boolean active;
	private List<String> enabledSegments;
	private List<String> enabledStrategies;

	// Modification for Angle One 2 Factor Auth support
	private String totpSecrete;
	private String TOTPEnabled;

	private Boolean systemUser;
	private String feedToken;
	private String refreshToken;
}
