package com.trade.algotrade.client.kotak.response;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;

import lombok.Data;

@Data
public class LtpResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	@JsonProperty("success")
	private List<LtpSuccess> success;

	@JsonProperty("fault")
	private Fault fault;
}
