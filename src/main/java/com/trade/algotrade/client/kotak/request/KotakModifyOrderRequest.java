package com.trade.algotrade.client.kotak.request;

import com.trade.algotrade.client.kotak.enums.ProductType;
import com.trade.algotrade.enums.ValidityEnum;
import com.trade.algotrade.enums.VarietyEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KotakModifyOrderRequest {
	private long quantity;
	private BigDecimal price;
	private ProductType product;
	private ValidityEnum validity;
	private VarietyEnum variety;
	private long disclosedQuantity;
	private BigDecimal triggerPrice;
	private String orderId;
}
