package com.trade.algotrade.enitiy.equity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * This class is entity which represents database table 'email_template'.
 *
 * @author suresh.thakare
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("stock_master")
@ToString
public class StockMaster {

    @Id
    private String id;
    private String stockSymbol;

    private BigDecimal ltp;

    private BigDecimal previousClose;

    private BigDecimal changePercent;

    private String segment;

    private String sectorName;

    private String stockName;

    private Long instrumentToken;

    private String isinCode;

    private LocalDateTime lastUpdatedAt;

    private boolean todaysTopGainer;

    private BigDecimal allTimeHigh;

    private BigDecimal changePercentRelativeAllTimeHigh;

    private boolean isNifty50Stock;

    private boolean isNifty100Stock;

    private boolean isNifty500Stock;

    private boolean isNiftyMidcap100Stock;

    private boolean isNiftySmallcap100Stock;

    List<StockDayCandle> weekHistory;


}
