package com.trade.algotrade.service;

import java.util.List;

import com.trade.algotrade.enitiy.StrategyEnity;
import com.trade.algotrade.request.OrderConfigUpdateRequest;
import com.trade.algotrade.request.StrategyRequest;
import com.trade.algotrade.response.StrategyResponse;

/**
 * @author suresh.thakare
 */
public interface StrategyService {

	StrategyResponse createStrategy(StrategyRequest strategyRequest);

	StrategyResponse getStrategy(String strategyName);

	StrategyResponse modifyStrategy(String startegyName, StrategyRequest strategyRequest);

	void deleteStrategy(String strategyName);
	
	List<StrategyEnity> getAllStrategies();

	void upadteOrderConfig(String strategyName, OrderConfigUpdateRequest orderConfigUpdateRequest);
}