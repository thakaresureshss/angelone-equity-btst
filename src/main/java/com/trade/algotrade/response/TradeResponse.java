package com.trade.algotrade.response;

import com.trade.algotrade.enitiy.BaseTradeEntity;
import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.enums.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document("open_trades")
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

    private String id;
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
    private Long tradeDuration;
    private BigDecimal averageTrendPoints;
    private Integer uniqueTrendCount;
    private List<OrderResponse> orders;

}
