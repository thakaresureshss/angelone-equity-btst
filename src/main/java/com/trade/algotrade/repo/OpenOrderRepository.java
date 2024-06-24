package com.trade.algotrade.repo;

import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enums.OrderCategoryEnum;
import com.trade.algotrade.enums.OrderStatus;
import com.trade.algotrade.enums.TransactionType;
import com.trade.algotrade.repo.filter.FilterableRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OpenOrderRepository extends MongoRepository<OpenOrderEntity, String>, FilterableRepository<OpenOrderEntity> {

    Optional<OpenOrderEntity> findByOrderId(Long orderId);

    List<OpenOrderEntity> findByUserIdAndOrderStatusAndCreatedAtGreaterThanEqual(String userId, OrderStatus orderStatus, LocalDateTime todayDate);

    List<OpenOrderEntity> findByOrderStatus(OrderStatus trad);

    List<OpenOrderEntity> findByOrderStatusAndCreatedAtGreaterThanEqual(OrderStatus orderStatus, LocalDateTime todayDate);

    List<OpenOrderEntity> findByOrderIdIn(List<Long> orderIds);

    List<OpenOrderEntity> findByOrderStatusAndOrderCategoryAndInstrumentToken(OrderStatus orderStatus, OrderCategoryEnum orderCategoryEnum, Long instrumentToken);

    Optional<OpenOrderEntity> findByOrderIdAndOrderStatusAndCreatedAtGreaterThanEqual(Long orderId, OrderStatus orderStatus, LocalDateTime todayDate);

    List<OpenOrderEntity> findByOrderStatusAndStrategyNameAndOrderCategory(OrderStatus orderStatus, String string, OrderCategoryEnum orderCategoryEnum);

    List<OpenOrderEntity> findByUserIdAndCreatedAtGreaterThanEqual(String userId, LocalDateTime todayDate);


    List<OpenOrderEntity> findByOrderCategoryAndIsSlOrderPlacedAndCreatedAtGreaterThanEqual(OrderCategoryEnum orderCategoryEnum, boolean isSlOrderPlaced, LocalDateTime marketOpenDateTime);

    List<OpenOrderEntity> findByInstrumentTokenAndOrderStatusAndCreatedAtGreaterThanEqual(Long instrumentToken, OrderStatus orderStatus, LocalDateTime marketOpenDateTime);

    List<OpenOrderEntity> findByUserIdAndIsSlOrderPlacedAndOrderStatus(String userId, boolean b, OrderStatus open);

    List<OpenOrderEntity> findByUserIdAndOrderCategoryAndOrderStatus(String userId, OrderCategoryEnum orderCategoryEnum, OrderStatus open);

    Optional<OpenOrderEntity> findByOriginalOrderIdAndTransactionType(Long originalOrderId, TransactionType transactionType);

    List<OpenOrderEntity> findByIsSlOrderPlacedAndCreatedAtGreaterThanEqual(boolean isSlOrderPlaced, LocalDateTime marketOpenDateTime);

    void deleteByOrderId(Long orderId);
}
