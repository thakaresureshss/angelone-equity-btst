package com.trade.algotrade.client.kotak.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OhlcSuccess implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long instrumentToken;
    public BigDecimal open;
    public String instrumentName;
    public BigDecimal high;
    public BigDecimal low;
    public BigDecimal close;
}