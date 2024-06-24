package com.trade.algotrade.enitiy;

import com.trade.algotrade.enums.CandleColor;
import com.trade.algotrade.enums.CandleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document("candle_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "stockSymbol_candleStart_idx", def = "{'stockSymbol' : 1, 'candleStart' : 1}", unique = true)
public class CandleEntity {

    private String stockSymbol;
    private String candleStart;
    private BigDecimal previousClose;

    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal ltp;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;

    private CandleColor color;
    private CandleType candleType;
    private boolean candleComplete;

    private BigDecimal candleBodyPoints;
    private BigDecimal candleTotalPoints;

    private BigDecimal gapBody;
    private BigDecimal changeBody;
}
