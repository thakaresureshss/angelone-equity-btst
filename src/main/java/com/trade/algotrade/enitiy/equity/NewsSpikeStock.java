package com.trade.algotrade.enitiy.equity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * This class is entity which represents database table 'email_template'.
 *
 * @author suresh.thakare
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("news_spike_stock")
@ToString
public class NewsSpikeStock {

    @Id
    private String id;

    private String stockSymbol;

    private String stockName;

    private BigDecimal changePercent;

    private BigDecimal tradePrice;

    private BigDecimal orderGainPercent;

    private Long instrumentToken;

    private String segment;

    private BigDecimal previousClose;

    private LocalDateTime tradeDate;

    private LocalDateTime squareOffDate;

    private BigDecimal squareOffDayHigh;

    private LocalDateTime createAt;

    private LocalDateTime updateAt;

}
