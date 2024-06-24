package com.trade.algotrade.enitiy;

import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.enums.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseTradeEntity {
    private String tradeId;
    private String userId;
    private Segment segment;
    private Long orderId;
    private TradeStatus tradeStatus;
    private Integer buyOpenQuantityToSquareOff;
    private Integer sellOpenQuantityToSquareOff;
    private Long instrumentToken;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String strategy;
    private BigDecimal realisedPnl;
    private String successStatus;
    private long tradeDuration;
    private BigDecimal averageTrendPoints;
    private int uniqueTrendCount;
    private BigDecimal maxTrendTickDifference;
    private int recoveryStrategyFailureCount;
}
