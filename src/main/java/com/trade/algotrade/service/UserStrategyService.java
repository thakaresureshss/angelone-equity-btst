package com.trade.algotrade.service;

import java.util.List;

import com.trade.algotrade.enitiy.UserStrategyEntity;
import com.trade.algotrade.request.OrderConfigUpdateRequest;

/**
 * @author suresh.thakare
 */

public interface UserStrategyService {

	List<UserStrategyEntity> findStrategyByUserId(String userId);

	UserStrategyEntity findStrategyByUserIdAndStrategy(String userId, String strategyName);

	void updateOrderConfig(String strategyName, OrderConfigUpdateRequest orderConfigUpdateRequest);

	UserStrategyEntity saveUserStrategy(UserStrategyEntity userStrategyEntity);
}