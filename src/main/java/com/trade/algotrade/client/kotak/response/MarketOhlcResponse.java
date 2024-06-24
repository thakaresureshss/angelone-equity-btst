package com.trade.algotrade.client.kotak.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.algotrade.client.kotak.dto.DepthSuccess;
import com.trade.algotrade.client.kotak.dto.Fault;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;
import com.trade.algotrade.client.kotak.dto.OhlcSuccess;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MarketOhlcResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    @JsonProperty("success")
    private List<OhlcSuccess> success;

    @JsonProperty("fault")
    private Fault fault;
}
