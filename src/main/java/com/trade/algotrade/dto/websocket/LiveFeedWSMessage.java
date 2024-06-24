package com.trade.algotrade.dto.websocket;

import java.math.BigDecimal;

import com.trade.algotrade.enums.OptionType;

import com.trade.algotrade.enums.MessageSource;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class LiveFeedWSMessage {
	private Long instrumentToken;
	private String instrumentName;
	private BigDecimal ltp;
	private String timeStamp;
	private BigDecimal changePercent;
	private BigDecimal previousDayClose;
	private OptionType optionType;
	private MessageSource messageSource;


}
