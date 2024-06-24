package com.trade.algotrade.client.kotak.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.KotakOrderDto;

import lombok.Data;

@Data
public class KotakGetOrderResponse {
	@JsonProperty("success")
	List<KotakOrderDto> success;

	@JsonProperty("fault")
	private Fault fault;
}