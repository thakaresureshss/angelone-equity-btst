package com.trade.algotrade.service;

import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enitiy.TradeEntity;
import com.trade.algotrade.enitiy.TradeHistoryEntity;
import com.trade.algotrade.enums.TradeStatus;
import com.trade.algotrade.request.TraderFilterQuery;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.TradeSummaryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * @author suresh.thakare
 */

public interface TradeService {

	List<TradeEntity> getTodaysTradeByUserId(String userId);

	TradeEntity createTrade(TradeEntity tradeOrderEntity);

	Optional<TradeEntity> getTradeByUserIdAndInstrumentTokenAndStrategyAndTradeStatus(String userId, Long instrumentToken, String strategyName, TradeStatus tradeStatus);

	List<TradeEntity> getTodaysOpenTradesByStrategyAndUserIdIn(String strategyname, List<String> userIds);

    BigDecimal getTodaysRealisedPnl(String userId);

	Optional<TradeEntity> getTradeById(String tradeId);

    void deleteTrade(TradeEntity newTradeCreated);

	TradeEntity modifyTrade(TradeEntity tradeOrderEntity);

	List<TradeEntity> getTradeByUserIdAndInstrumentTokenAndStrategy(String userId, Long instrumentToken, String strategyName);
	
	List<OpenOrderEntity> getOpenOrderList(String tradId);
	
	Optional<TradeEntity> getTradeForOrder(String orderId);

	boolean isOpenTradesFoundForToday();

    List<TradeHistoryEntity> moveTradesToHistory();

	List<TradeEntity> getTradesByInstrumentAndStrategyAndStatus(Long instrument, String strategy);

	List<TradeEntity> getAllTodaysCompletedTrades(String userId);

	List<TradeEntity> getOpenTradesFoundForToday();

	List<TradeEntity> getAllTodaysTradesOrderByDesc(String userId);

	TradeSummaryResponse getTradeSummary(TraderFilterQuery traderFilterQuery);
}