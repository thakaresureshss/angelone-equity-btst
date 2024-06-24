package com.trade.algotrade.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.trade.algotrade.enitiy.CandleEntity;

public interface CandleService {

	Optional<CandleEntity> findBySymbolLastCandle(String symbol);

	void saveCandle(CandleEntity candle);

	CandleEntity buildCandleBankNiftyCandles(String symbol, BigDecimal value);

	List<CandleEntity> findBySymbol(String symbol);

	CandleEntity buildCandles(String symbol, BigDecimal value);

	List<CandleEntity> findCandlesBySymbolsDescending(String symbol);
	
	void generateTimeSlotUsingTimeFrame();

	BigDecimal latestTrendFinder();

	void clearTrendData();

	void generateTrendData(BigDecimal value);
}
