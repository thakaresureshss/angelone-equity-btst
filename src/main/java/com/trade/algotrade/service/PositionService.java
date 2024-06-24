package com.trade.algotrade.service;

import java.util.List;

import com.trade.algotrade.dto.PositionDto;
import com.trade.algotrade.enums.Segment;

/**
 * @author suresh.thakare
 */
public interface PositionService {

	List<PositionDto> getTodaysPositions(String userId);

	List<PositionDto> getTodaysEquityPosition(String userId);

	List<PositionDto> getTodaysOpenEquityPosition(String userId);


	List<PositionDto> getOpenPosition(String userId, Segment segment);

	void squareOffOpenPositions(Segment segment);

}