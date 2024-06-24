package com.trade.algotrade.service;

import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enitiy.TradeHistoryEntity;
import com.trade.algotrade.enums.OrderCategoryEnum;
import com.trade.algotrade.enums.OrderStatus;
import com.trade.algotrade.request.OrderFilterQuery;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.UserResponse;

import java.util.List;

/**
 * @author suresh.thakare
 */
public interface OrderService {
    List<OrderResponse> createOrder(OrderRequest orderRequest);

    List<OrderResponse> modifyOrder(Long orderId, OrderRequest orderRequest, String squareOffReason, boolean directOrder);

    OrderResponse getOrder(Long orderId, String userId);

    void cancelOrder(Long orderId, String userId);

    List<OrderResponse> getAllOrders(String userId);

    void moveOrdersToHistory(List<TradeHistoryEntity> tradeHistoryEntities);

    List<OrderResponse> getOpenOrders();


    List<OpenOrderEntity> getOpenOrdersByOrderIds(List<Long> orderIds);

    OpenOrderEntity saveOpenOrders(OpenOrderEntity openOrderEntity);

    List<OpenOrderEntity> getTodaysOpenOrdersByUserId(String userId);

    boolean modifySlOrderToMarketOrder(OpenOrderEntity o, String squareOffReason);

    List<OpenOrderEntity> getTodaysAllOrdersByUserId(String userId);

    List<OpenOrderEntity> getTodaysOrdersByStatus(OrderStatus orderStatus);

    OpenOrderEntity getOpenOrderByOrderId(Long orderId);

    List<OrderResponse> prepareOrderAndCreateOrder(OrderRequest orderRequest);

    OpenOrderEntity getOpenSquareOffOrderByInstrumentToken(Long tickerInstrumentToken);

    void cancelExpiryOrder();

    void squareOffOrder(String strategyName);

    List<OpenOrderEntity> getTodaysOrdersByCategory(OrderCategoryEnum orderCategoryEnum, boolean isSlOrderPlaced);

    List<OrderResponse> getAllOrdersByFilter(OrderFilterQuery orderFilter);


    List<OpenOrderEntity> getTodaysOrdersByInstrumentTokenAndStatus(Long instrumentToken, OrderStatus orderStatus);

    void modifyOrderFromController();

    public Integer getOrderQuantityForStrategy(String userId, String strategyName);


    OpenOrderEntity findOrderByOrderId(Long orderId);

    List<OpenOrderEntity> getAllOpenSLOrdersByUserId(String userId);

    Boolean modifySLOrderStatusAndPrice(OpenOrderEntity orderEntity, String currentOrderStatus);

    OpenOrderEntity getSLOrderByOriginalOrderId(Long originalOrderId);

    List<OpenOrderEntity> getOrdersByIsSLPlaced(boolean flag);

    OpenOrderEntity placeDirectSLOrder(OrderRequest orderRequest, UserResponse userResponse);
}