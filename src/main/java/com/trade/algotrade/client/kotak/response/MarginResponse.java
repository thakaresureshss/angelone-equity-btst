package com.trade.algotrade.client.kotak.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.MarginSuccessDto;

import lombok.Data;

@Data
public class MarginResponse {
	@JsonProperty("Success")
	private MarginSuccessDto success;

	@JsonProperty("fault")
	private Fault fault;
}
