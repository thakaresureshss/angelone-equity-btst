package com.trade.algotrade.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enitiy.TradeOrderEntity;

@Repository
public interface TradeOrderRepository extends MongoRepository<TradeOrderEntity, String> {

    List<TradeOrderEntity> findByTradeId(String tradeId);
    @Query("SELECT ooe FROM TradeOrderEntity toe,"
    		+ " LEFT JOIN FETCH toe.orderId ooe"
    		+ " WHERE toe.id =:tradeId")
    List<OpenOrderEntity> getAllOpenOrdersByTradeId(@Param("tradeId") String tradeId);
}