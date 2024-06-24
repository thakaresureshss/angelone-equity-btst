package com.trade.algotrade.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
public class BigCandle {
    LocalDateTime dateTime;
    BigDecimal ltp;
}
