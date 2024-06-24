package com.trade.algotrade.service;

import com.trade.algotrade.request.ManualOrderRequest;
import com.trade.algotrade.response.OrderResponse;
import org.springframework.stereotype.Component;

import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.dto.websocket.OrderFeedWSMessage;
import com.trade.algotrade.enums.Strategy;

import java.util.List;

@Component
public interface StrategyBuilderService {

	void buildBigCandleStrategy(LiveFeedWSMessage webSocketMessage, Strategy strategy);

	void processOrderFeed();

	void monitorPositions(LiveFeedWSMessage liveFeedMessage);	

	void addSLAndMarginForPlacedOrder(OrderFeedWSMessage orderFeedWSMessage);

	boolean ignoreActionOnOrderFeedStatues(OrderFeedWSMessage orderFeedWSMessage);

	void buildBigMoveStrategy();

    List<OrderResponse> createManualOrder(ManualOrderRequest orderRequest);

	void processSLOrderFeed(OrderFeedWSMessage orderFeedWSMessage);

	List<OrderResponse> prepareMockOrder(ManualOrderRequest orderRequest);
}
