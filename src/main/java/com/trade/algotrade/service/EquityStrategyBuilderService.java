package com.trade.algotrade.service;

import com.trade.algotrade.enitiy.equity.StockMaster;
import org.springframework.stereotype.Component;

import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.dto.websocket.OrderFeedWSMessage;
import com.trade.algotrade.enums.Strategy;

import java.util.List;

@Component
public interface EquityStrategyBuilderService {

   // void buildEquityStrategy(LiveFeedWSMessage message, Strategy equityTopMovers);

    void buildEquityStrategy(List<StockMaster> message, Strategy strategy);
   // void monitorEqutiyOrders(OrderFeedWSMessage message);

}
