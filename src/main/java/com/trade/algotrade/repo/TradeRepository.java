package com.trade.algotrade.repo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.repo.filter.FilterBuilder;
import com.trade.algotrade.repo.filter.FilterableRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.TradeEntity;
import com.trade.algotrade.enums.TradeStatus;

@Repository
public interface TradeRepository extends MongoRepository<TradeEntity, String>, FilterableRepository<TradeEntity> {

    List<TradeEntity> findByUserIdAndCreatedTimeGreaterThanEqual(String userId, LocalDateTime todaysDate);

    List<TradeEntity> findByUserIdAndInstrumentTokenAndTradeStatusAndCreatedTimeGreaterThanEqual(String userId, Long instrumentToken, TradeStatus tradeStatus, LocalDateTime todaysDate);

    List<TradeEntity> findByStrategyAndUserIdInAndTradeStatusAndCreatedTimeGreaterThanEqual(String strategyname, List<String> userIds, TradeStatus tradeStatus, LocalDateTime todaysDate);

    Optional<TradeEntity> findByTradeId(String tradeId);

    Optional<TradeEntity> findByStrategyAndUserIdAndTradeStatusAndCreatedTimeGreaterThanEqualAndInstrumentToken(String strategyName, String userId, TradeStatus tradeStatus, LocalDateTime marketOpenTime, Long instrumentToken);

    @Query("{'orders.id':?0}")
    Optional<TradeEntity> getTradeForOrder(String orderId);

    List<TradeEntity> findByTradeStatusAndCreatedTimeGreaterThanEqual(TradeStatus tradeStatus, LocalDateTime marketOpenDateTime);

    List<TradeEntity> findByTradeStatus(TradeStatus tradeStatus);

    List<TradeEntity> findByInstrumentTokenAndStrategyAndTradeStatusAndCreatedTimeGreaterThanEqual(Long instrument, String strategy, TradeStatus tradeStatus, LocalDateTime todaysDate);

    List<TradeEntity> findByUserIdAndTradeStatusAndCreatedTimeGreaterThanEqual(String userId, TradeStatus tradeStatus, LocalDateTime marketOpenDateTime);

    List<TradeEntity> findByUserIdAndTradeStatusAndCreatedTimeGreaterThanOrderByCreatedTimeDesc(String userId, TradeStatus tradeStatus, LocalDateTime marketOpenDateTime);

}
