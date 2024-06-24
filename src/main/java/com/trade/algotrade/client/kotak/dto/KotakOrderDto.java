package com.trade.algotrade.client.kotak.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class KotakOrderDto {
	private Long orderId;
	private String orderTimestamp;
	private Integer cancelledQuantity;
	private Long disclosedQuantity;
	private String exchangeOrderID;
	private String expiryDate;
	private Integer filledQuantity;
	private String instrumentName;
	private Long instrumentToken;
	private String instrumentType;
	private String isFNO;
	private String optionType;
	private Integer orderQuantity;
	private Integer pendingQuantity;
	private BigDecimal price;
	private String product;
	private String status;
	private String statusInfo;
	private String statusMessage;
	private BigDecimal strikePrice;
	private String tag;
	private String transactionType;
	private BigDecimal triggerPrice;
	private String validity;
	private String variety;
	public String exchangeOrderId;
	public int leg;
	public String marketExchange;
	public int marketLot;
	public int multiplier;
	public String activityTimestamp;
	public String exchOrderId;
	public String exchTradeId;
	public String exchangeStatus;
	public String message;
}
