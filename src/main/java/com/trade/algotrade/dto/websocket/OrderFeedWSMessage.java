package com.trade.algotrade.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.trade.algotrade.enums.MessageSource;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class OrderFeedWSMessage {

	// Used Fields
	@JsonProperty("UserId")
	private String userId;

	@JsonProperty("BuySell")
	private String buySell;

	@JsonProperty("Quantity")
	private String quantity;

	@JsonProperty("Price")
	private String price;

	@JsonProperty("orderid") // This field is mapped to order ID
	private String orderId;

	// Date
	@JsonProperty("Date")
	private String date;

	@JsonProperty("Token")
	private Long instrumentToken;

	@JsonProperty("Status")
	private String status;

	// Non Used fields
	private String msgType;
	private String marketExchange;
	private String instrumentType;
	private String clientGroup;
	private String abwsTradeNo;
	private String triggerPrice;
	private String deliverymarkStatus;
	private String exchgOrderNo;
	private String exchangeTradeNo;
	private String orderQty;
	private String avgPrice;
	private String ordSource;
	private String variety;
	private String type;
	@JsonProperty("Header_TransactionCode")
	private String headerTransactionCode;
	private MessageSource messageSource;
	private String orderTag;
}
