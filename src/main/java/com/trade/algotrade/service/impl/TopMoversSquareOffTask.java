package com.trade.algotrade.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.trade.algotrade.dto.PositionDto;
import com.trade.algotrade.enitiy.TradeEntity;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.OrderService;
import com.trade.algotrade.service.PositionService;
import com.trade.algotrade.service.TradeService;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.utils.DateUtils;

class TopMoversSquareOffTask implements Runnable {

	private final Logger logger = LoggerFactory.getLogger(TopMoversSquareOffTask.class);

	PeriodicExecutorService periodicExecutorService;

	@Autowired
	PositionService positionService;

	@Autowired
	UserService userService;

	@Autowired
	OrderService orderService;

	@Autowired
    TradeService tradeOrderService;

	public TopMoversSquareOffTask(PeriodicExecutorService periodicExecutorService) {
		super();
		this.periodicExecutorService = periodicExecutorService;
	}

	@Override
	public void run() {
		logger.info("TopMoversSquareOffTask Started ");
		List<UserResponse> allActiveSegmentEnabledUsers = userService.getAllActiveSegmentEnabledUsers(Segment.EQ);
		if (!CollectionUtils.isEmpty(allActiveSegmentEnabledUsers)) {
			allActiveSegmentEnabledUsers.forEach(user -> {
				List<PositionDto> todaysOpenEquityPosition = positionService
						.getTodaysOpenEquityPosition(user.getUserId());
				if (CollectionUtils.isEmpty(todaysOpenEquityPosition)) {
					periodicExecutorService.cancelTask();
				}
				BigDecimal todayPositionsRealizedPnl = todaysOpenEquityPosition.stream().map(x -> x.getRealizedPL())
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal maximumEquityLossPerDay = user.getDayLossLimit().get(Segment.EQ.toString());
				BigDecimal maximumProfitPerDay = user.getDayProfitLimit().get(Segment.EQ.toString());
				if (maximumEquityLossPerDay.compareTo(todayPositionsRealizedPnl) >= 0
						|| maximumProfitPerDay.compareTo(todayPositionsRealizedPnl) >= 0
						|| DateUtils.isIntradaySquareOffTime()) {
					List<TradeEntity> tradeByUserId = tradeOrderService.getTodaysTradeByUserId(user.getUserId());
					todaysOpenEquityPosition.forEach(typ -> {
						Optional<TradeEntity> algoOpenOrderDto = tradeByUserId.stream()
								.filter(oo -> oo.getInstrumentToken().compareTo(typ.getInstrumentToken()) == 0)
								.findFirst();
						OrderRequest orderRequest = new OrderRequest();
						// TODO build squre off sa
						orderService.createOrder(orderRequest);

					});
					periodicExecutorService.cancelTask();
				}

			});
		}
		logger.info("TopMoversSquareOffTask Completed ");
	}
}