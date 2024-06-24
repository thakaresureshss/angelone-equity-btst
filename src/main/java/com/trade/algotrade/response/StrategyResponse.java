package com.trade.algotrade.response;

import java.math.BigDecimal;

import com.trade.algotrade.dto.EntryCondition;
import com.trade.algotrade.dto.ExitCondition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyResponse {

	private String id;

	private String strategyName;

	private ExitCondition exitCondition;

	private EntryCondition entryCondition;

	private Integer noOfTargetCompletedOrders;

	private Integer noOfSlOrders;

	private BigDecimal successRatio;
}