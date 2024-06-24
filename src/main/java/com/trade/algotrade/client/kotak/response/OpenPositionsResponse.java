package com.trade.algotrade.client.kotak.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.OpenPositionsSuccess;

import lombok.Data;

import java.util.List;

@Data
public class OpenPositionsResponse {

	@JsonProperty("Success")
	private List<OpenPositionsSuccess> success;

	@JsonProperty("fault")
	private Fault fault;
}