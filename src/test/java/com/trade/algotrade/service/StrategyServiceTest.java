package com.trade.algotrade.service;

import com.trade.algotrade.service.impl.StrategyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StrategyServiceTest {

	@InjectMocks
	StrategyService strategyService = new StrategyServiceImpl();

	@BeforeEach
	void setUp() throws Exception {
		
		
	}


}
