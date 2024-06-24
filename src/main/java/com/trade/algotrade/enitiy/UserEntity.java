package com.trade.algotrade.enitiy;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("user_master")
@ToString
public class UserEntity {

	@Id
	private Long id;
	@Indexed(unique = true)
	private String userId;
	private String password;
	private String accessToken;
	private String broker;
	private String appId;
	private String consumerKey;
	private String consumerSecrete;//Used for only Kotak
	private String exchange;
	private String oneTimeToken;
	private String websocketToken;
	private Map<String, BigDecimal> dayProfitLimit;
	private Map<String, BigDecimal> dayLossLimit;
	private Map<String, Integer> minTradesPerDay;
	private Map<String, Integer> maxTradesPerDay;
	private Boolean isRealTradingEnabled;
	private Boolean active;
	private List<String> enabledSegments;
	private List<String> enabledStrategies;
	private LocalDateTime createdTime;
	private LocalDateTime updatedTime;

	// Modification for Angle One 2 Factor Auth support
	private String totpSecrete;
	private String TOTPEnabled;
	private String feedToken;
	private String refreshToken;
	private String sessionToken;
	private LocalDateTime lastLogin;
	private Boolean systemUser;
	private String telegramChatId;


}
