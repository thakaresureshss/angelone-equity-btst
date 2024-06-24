package com.trade.algotrade.constants;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Constants {
	public static final String BANK_NIFTY = "BANKNIFTY";
	public static final String BANK_NIFTY_INDEX = "NIFTY BANK";
	public static final String INDIA_VIX_INDEX = "India VIX";
	public static final String NSE = "NSE";
	public static final String NFO = "FO";
	public static final String INDEX = "IN";
	public static final String BIG_CANDLE_STRATEGY = "BIGCANDLE";
	public static final String STRATEGY_EXPIRY = "EXPIRY";
	public static final String STRATEGY_MANUAL = "MANUAL";
	public static final BigDecimal ONE_POINT_TWENTY_FIVE = new BigDecimal("1.25");
	public static final String DEFAULT_QUANTITY = "DEFAULT_QUANTITY";
	public static final String LAST_ORDER_QUANTITY = "LAST_ORDER_QANTITY";
	public static final String MESSAGE_GET_DATA = "getdata";
	public static final String BIG_CANDLE_POINTS = "BIG_CANDLE_POINTS";
	public static final String EQUITY = "EQ";
	public static final String STRATEGY_EQ_TOP_MOVER = "EQ_TOP_MOVER";
	public static final String BIG_CANDLE_REVERSE_POINTS = "BIG_CANDLE_REVERSE_POINTS";
	public static final String DEFAULT_SL_POINT = "DEFAULT_SL_POINT";
	public static final String DEFAULT_SL_PERCENT = "DEFAULT_SL_PERCENT";
	public static final String DEFAULT_SL_BUFFER_POINT = "DEFAULT_SL_BUFFER_POINT";
	public static final String DEFAULT_SL_BUFFER_PERCENT = "DEFAULT_SL_BUFFER_PERCENT";
	public static final String DEFAULT_TARGET_PERCENT = "DEFAULT_TARGET_PERCENT";


	public static final String SEGMENT_OPTIONS ="OI";
	public static final String DEFAULT_SL_SPREAD = "DEFAULT_SL_SPREAD";
	public static final String DEFAULT_SL_SPREAD_PERCENT = "DEFAULT_SL_SPREAD_PERCENT";


	public static final BigDecimal THREE_HUNDRED_PERCENT = new BigDecimal(300);
	public static final BigDecimal DEFAULT_SL_FIFTY_PERCENT = new BigDecimal(50);


	public static final String LAST_BANK_NIFTY_INDEX_VALUE ="LAST_BANK_NIFTY_INDEX_VALUE" ;
	public static final String MARKET_MIN_SL_PERCENT = "MARKET_MIN_SL_PERCENT";
	public static final String BROKER_MAX_MODIFICATION_COUNT = "BROKER_MAX_MODIFICATION_COUNT";

	public static final String TRADE_SUCCESSFUL = "SUCCESSFUL";
	public static final String TRADE_UNSUCCESSFUL = "UNSUCCESSFUL";


	public static final Integer MAX_RETRY_DOWNLOAD_INSTRUMENT = 3;

	public static final Integer MAX_RETRY_SESSION_TOKEN = 3;

	public static final Integer MAX_RETRY_OTT_TOKEN = 2;
	// This MAX_RETRY_OTT_TOKEN config is changed to as wrong 3 attempt locks the users

	public static final Integer MAX_RETRY_PLACE_ORDER = 3;
	public static final String MONTHLY_EXPIRY_DAY = "MONTHLY_EXPIRY_DAY";
	public static final String WEEKLY_EXPIRY_DAY = "WEEKLY_EXPIRY_DAY";

	public static final String BIG_CANDLE_TREND_FINDER_TIME_GAP = "BIG_CANDLE_TREND_FINDER_TIME_GAP";

	public static final String BROKER_KOTAK = "Kotak";

	public static final int GENERIC_DEFAULT_QUANTITY = 50;
	public static final BigDecimal DEFAULT_SL_POINT_VALUE = new BigDecimal(30);
	public static final String NIFTY_INDEX = "NIFTY 50";
	public static final String NIFTY = "NIFTY";
	public static final BigDecimal DECIMAL_TWO = new BigDecimal(2);
	public static final String STOP_LOSS_POINTS = "STOP_LOSS_POINTS";
	public static final String STRATEGY_EXPIRY_SCALPING = "STRATEGY_EXPIRY_SCALPING";
	public static final String STRATEGY_SCALPING = "STRATEGY_SCALPING";
	public static final String MODIFY_ORDER_ERROR_IF_ALREADY_FIL = "Order Modification not allowed as current Order status is FIL,";

	public static final String FINNIFTY = "FINNIFTY";

	public static final String ALGOTRADE_TAG = "AlgotradeTag";
}
