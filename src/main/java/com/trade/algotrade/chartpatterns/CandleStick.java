package com.trade.algotrade.chartpatterns;

public interface CandleStick{
		/**
		 * Call it only, if you are in a <b>downtrend.</b><p>
		 * <b>Suggestion:</b> Overlap EMA's. <b>Example:</b> EMA8, EMA13 and EMA21.</br>
		 * If EMA13 is over EMA8 and EMA21 is over EMA13, than you are in a downtrend.
		 * </p>
		 * @return true if bullish, false otherwise.
		 */
		public boolean isBullish();
		/**
		 * Call it only, if you are in a <b>uptrend.</b><p>
		 * <b>Suggestion:</b> Overlap EMA's. <b>Example:</b> EMA8, EMA13 and EMA21.</br>
		 * If EMA13 is over EMA21 and EMA8 is over EMA13, than you are in a uptrend.
		 * </p>
		 * @return true if bearish, false otherwise.
		 */
		public boolean isBearish();
	}