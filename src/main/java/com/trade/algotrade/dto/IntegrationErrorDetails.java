package com.trade.algotrade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntegrationErrorDetails {

	@JsonProperty("fault")
	private Fault fault;
}