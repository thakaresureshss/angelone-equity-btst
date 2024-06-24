package com.trade.algotrade.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class
UserResponse {

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
	private Boolean isRealTradingEnabled = false;
	private Boolean active = false;
	private List<String> enabledStrategies;
	private List<String> enabledSegments;

	// Modification for Angle One 2 Factor Auth support
	private String totpSecrete;
	private String TOTPEnabled;
	private String refreshToken;
	private String feedToken;
	private Boolean systemUser;
	private LocalDateTime lastUpdatedAt;
	private LocalDateTime lastLogin;
	private String telegramChatId;
}
