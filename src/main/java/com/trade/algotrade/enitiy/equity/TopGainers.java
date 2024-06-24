package com.trade.algotrade.enitiy.equity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * This class is entity which represents database table 'email_template'.
 * 
 * @author suresh.thakare
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("topgainer_stocks")
@ToString
public class TopGainers {

	@Id
	private String id;

	private String stockSymbol;

	private String stockName;

	private Long instrumentToken;

	private BigDecimal ltp;

	private String sectorName;

	private boolean isNifty50Stock;

	private String isinCode;

	private String segment;

	private LocalDateTime lastUpdatedAt;

	private BigDecimal changePercent;

	private BigDecimal yesterdayClose;

	private boolean todaysTopGainer;

}
