package com.trade.algotrade.client.kotak.dto;

import lombok.Data;

@Data
public class Fault {
	private long code;
	private String description;
	private String message;
}