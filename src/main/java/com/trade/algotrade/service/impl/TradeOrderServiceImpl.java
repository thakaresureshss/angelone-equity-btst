package com.trade.algotrade.service.impl;

import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enitiy.TradeOrderEntity;
import com.trade.algotrade.repo.TradeOrderRepository;
import com.trade.algotrade.service.TradeOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeOrderServiceImpl implements TradeOrderService {

    @Autowired
    TradeOrderRepository tradeOrderRepository;

    @Override
    public void createTradeOrders(List<TradeOrderEntity> tradeOrders) {
        tradeOrderRepository.saveAll(tradeOrders);
    }

    @Override
    public List<TradeOrderEntity> findOrdersByTradeId(String tradeId) {
        return tradeOrderRepository.findByTradeId(tradeId);
    }
    
    @Override
	public List<OpenOrderEntity> getAllOrdersByTradeId(String tradeId) {
		return tradeOrderRepository.getAllOpenOrdersByTradeId(tradeId);
	}
}
