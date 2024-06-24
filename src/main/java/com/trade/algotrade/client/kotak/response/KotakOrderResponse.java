package com.trade.algotrade.client.kotak.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.CreateOrderDto;
import com.trade.algotrade.client.kotak.dto.Fault;

import lombok.Data;

@Data
public class KotakOrderResponse {

	@JsonProperty("Success")
	private Map<String, CreateOrderDto> success;

	@JsonProperty("fault")
	private Fault fault;
}