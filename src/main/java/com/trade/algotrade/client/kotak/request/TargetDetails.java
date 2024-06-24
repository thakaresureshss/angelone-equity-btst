package com.trade.algotrade.client.kotak.request;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TargetDetails {
	private BigDecimal targetPrice;
	private BigDecimal targetPercent;
	private boolean target;
}
