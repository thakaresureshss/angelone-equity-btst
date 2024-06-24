package com.trade.algotrade.response;

import com.trade.algotrade.client.kotak.dto.CreateOrderDto;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.enitiy.TradeOrderEntity;
import com.trade.algotrade.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TradeSummaryResponse {

    private Integer totalTrades;

    private Integer minTrendUniqueCount;
    private Integer maxTrendUniqueCount;
    private Integer avgTrendUniqueCount;


    private Long minTradeDuration;
    private Long maxTradeDuration;
    private Long avgTradeDuration;

    private Long minLossPoint;
    private Long maxLossPoint;
    private Long avgLossPoint;

    private Long minProfitPoint;
    private Long maxProfitPoint;
    private Long avgProfitPoint;

    private BigDecimal totalPnl;

    private Integer successTrade;
    private Integer failedTrades;

    List<TradeResponse> trades;
}