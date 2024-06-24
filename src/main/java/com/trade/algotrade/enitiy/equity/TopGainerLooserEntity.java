package com.trade.algotrade.enitiy.equity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.trade.algotrade.enums.MoversType;

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
@Document("top_looser_gainer")
@ToString
public class TopGainerLooserEntity {

	@Id
	private String id;
	private String stockSymbol;

	private MoversType type;

	private BigDecimal changePercent;

	private BigDecimal ltp;

	private BigDecimal previousClose;

	private LocalDateTime createdAt;

	private LocalDateTime lastUpdatedAt;


	private String stockName;

	private Long instrumentToken;

	private boolean isNifty50Stock;

	private String segment;

	private boolean todaysTopGainer;


}
