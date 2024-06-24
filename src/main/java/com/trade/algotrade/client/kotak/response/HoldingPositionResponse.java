package com.trade.algotrade.client.kotak.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.HoldingPositionSuccess;

import lombok.Data;

@Data
public class HoldingPositionResponse {

	private List<HoldingPositionSuccess> success;

	@JsonProperty("fault")
	private Fault fault;

}