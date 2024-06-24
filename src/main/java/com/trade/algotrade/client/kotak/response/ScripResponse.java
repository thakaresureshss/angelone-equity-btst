package com.trade.algotrade.client.kotak.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.ScripSuccess;

import lombok.Data;

@Data
public class ScripResponse {

	@JsonProperty("Success")
	private ScripSuccess Success;

	@JsonProperty("fault")
	private Fault fault;
}