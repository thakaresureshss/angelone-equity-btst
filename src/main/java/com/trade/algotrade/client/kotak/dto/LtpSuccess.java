package com.trade.algotrade.client.kotak.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class LtpSuccess implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long instrumentToken;
    public BigDecimal lastPrice;
    public String instrumentName;

    //Upper circuit Limit
    public String upper_ckt_limit;
    //Lower circuit Limit
    public String lower_ckt_limit;
    // Segment
    public String stk_it;
}