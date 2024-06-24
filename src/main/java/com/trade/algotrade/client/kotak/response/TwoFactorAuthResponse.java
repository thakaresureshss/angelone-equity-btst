package com.trade.algotrade.client.kotak.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.Success;

import lombok.Data;

@Data
public class TwoFactorAuthResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private Success success;

	@JsonProperty("fault")
	private Fault fault;

}
