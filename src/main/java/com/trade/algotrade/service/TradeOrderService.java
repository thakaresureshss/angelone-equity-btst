package com.trade.algotrade.service;

import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enitiy.TradeOrderEntity;

import java.util.List;

/**
 * @author suresh.thakare
 */

public interface TradeOrderService {


    void createTradeOrders(List<TradeOrderEntity> tradeOrderMappings);

    List<TradeOrderEntity> findOrdersByTradeId(String tradeId);
    
    List<OpenOrderEntity> getAllOrdersByTradeId(String tradeId);
}