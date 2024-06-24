package com.trade.algotrade.client.angelone.request.orders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.angelone.enums.ExchangeType;
import com.trade.algotrade.client.angelone.enums.ProductType;
import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.enums.OrderType;
import com.trade.algotrade.enums.TransactionType;
import com.trade.algotrade.enums.ValidityEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AngelOrderRequest {
	@JsonProperty("transactiontype")
	private TransactionType transactionType;
	private long quantity;
	
	private BigDecimal price;
	@JsonProperty("producttype")
	private ProductType product;
	private VarietyEnum variety;
	@JsonProperty("triggerprice")
	private BigDecimal triggerPrice;
	@JsonProperty("orderid")
	private Long orderId;
	@JsonProperty("tradingsymbol")
	private String tradingSymbol;
	@JsonProperty("symboltoken")
	private String symbolToken;
	private ExchangeType exchange;
	@JsonProperty("ordertype")
	private OrderType ordertype;
	private String duration;
	@JsonProperty("squareoff")
	private String squareOff;
	@JsonProperty("stoploss")
	private String stopLoss;

	@JsonIgnore
	private boolean isMockOrder;
	@JsonProperty("ordertag")
	private String orderTag;
}
