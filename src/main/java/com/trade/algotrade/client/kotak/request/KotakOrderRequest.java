package com.trade.algotrade.client.kotak.request;

import java.math.BigDecimal;

import com.trade.algotrade.client.angelone.enums.ProductType;
import com.trade.algotrade.enums.TransactionType;
import com.trade.algotrade.enums.ValidityEnum;
import com.trade.algotrade.enums.VarietyEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KotakOrderRequest {
	private Long instrumentToken;
	private TransactionType transactionType;
	private long quantity;
	
	private BigDecimal price;
	private ProductType product;
	private ValidityEnum validity;
	private VarietyEnum variety;
	private long disclosedQuantity;
	private BigDecimal triggerPrice;
	private String tag;
	private Long orderId;
}
