package com.trade.algotrade.enitiy;

import com.trade.algotrade.dto.EntryCondition;
import com.trade.algotrade.dto.ExitCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document("strategy_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyEnity {
	@Id
	private String id;

	@Indexed(unique = true)
	private String strategyName;

	private ExitCondition exitCondition;

	private EntryCondition entryCondition;

	private Integer noOfTargetCompletedOrders;

	private Integer noOfSlOrders;

	private BigDecimal successRatio;

	private LocalDateTime createdTime;
	private LocalDateTime updatedTime;
}