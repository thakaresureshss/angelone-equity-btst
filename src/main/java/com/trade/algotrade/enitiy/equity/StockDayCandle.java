package com.trade.algotrade.enitiy.equity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * This class is entity which represents database table 'email_template'.
 *
 * @author suresh.thakare
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class StockDayCandle {
    LocalDate date;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal changePercent;
    private BigDecimal previousClose;


}
