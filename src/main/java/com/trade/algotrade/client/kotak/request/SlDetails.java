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
public class SlDetails {
	private BigDecimal spread;
	private BigDecimal triggerPrice;
	private BigDecimal slPrice;
	private BigDecimal trailingStartPrice;
	private boolean trailSl;
	private BigDecimal slPercent;
	private BigDecimal targetPercent;
	private BigDecimal slBufferPercent;
	private BigDecimal slPoints;
	private BigDecimal slBufferPoints;
	private BigDecimal slNextTargetPrice;
	private BigDecimal targetPrice;
}
