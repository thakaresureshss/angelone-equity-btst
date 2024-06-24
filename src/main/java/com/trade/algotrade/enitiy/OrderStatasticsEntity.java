package com.trade.algotrade.enitiy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@Document("order_statastics")
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatasticsEntity {

	@Id
	private Long instrumentToken;
	private String userId;
	private Integer strikePrice;
	private BigDecimal profitPrice;
	private BigDecimal lossPrice;
	private LocalDateTime orderDate;
	private Integer noOfTrades;
	
	
}
