package com.trade.algotrade.client.kotak.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CreateOrderDto {
	private String message;
	private Long orderId;
	private BigDecimal price;
	private Integer quantity;
	private String tag;
}