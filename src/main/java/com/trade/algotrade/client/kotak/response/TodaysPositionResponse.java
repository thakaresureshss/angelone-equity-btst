package com.trade.algotrade.client.kotak.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.TodaysPositionSuccess;

import lombok.Data;

@Data
public class TodaysPositionResponse {

	private List<TodaysPositionSuccess> success;

	@JsonProperty("fault")
	private Fault fault;

}