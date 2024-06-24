package com.trade.algotrade.constants;

import org.springframework.stereotype.Component;

@Component
public class ConfigConstants {
    public static final String KOTAK_ORDER_QUANTITY_SIZE = "KOTAK_SINGLE_ORDER_QUANTITY_LIMIT";
    public static final String KOTAK_MAX_ORDER_MODIFICATION_COUNT = "KOTAK_MAX_ORDER_MODIFICATION_COUNT";
    public static final String BIG_CANDLE_TREND_FINDER_TIME_FRAME = "BIG_CANDLE_TREND_FINDER_TIME_FRAME";
    public static final String MARKET_MIN_SL_PERCENT = "MARKET_MIN_SL_PERCENT";

    public static final String BIG_CANDLE_TRADING_START_TIME = "BIG_CANDLE_TRADING_START_TIME";
    public static final String BIG_CANDLE_TRADING_END_TIME = "BIG_CANDLE_TRADING_END_TIME";

    public static final String CANDLE_DATA_CAPTURE_START_TIME = "CANDLE_DATA_CAPTURE_START_TIME";

    public static final String MARKET_OPEN_TIME = "MARKET_OPEN_TIME";
    public static final String MOCK_BANK_NIFTY_CURRENT_VALUE = "MOCK_BANK_NIFTY_CURRENT_VALUE";
    public static final String NEW_SPIKE_STOCKS_COUNT = "NEW_SPIKE_STOCKS_COUNT";
    public static final String NEW_SPIKE_CHANGE_PERCENT = "NEW_SPIKE_CHANGE_PERCENT";
    public static final String STOCK_PRICE_HISTORY_DAYS = "STOCK_PRICE_HISTORY_DAYS";
    public static final String EQUITY_LTP_BATCH_SIZE = "EQUITY_LTP_BATCH_SIZE";

    public static final String INSTRUMENT_DOWNLOAD_BATCH_SIZE = "INSTRUMENT_DOWNLOAD_BATCH_SIZE";
    public static final String WS_DEFAULT_PING_INTERVAL_MS = "WS_DEFAULT_PING_INTERVAL_MS";
    public static final String ORDER_FEED_WS_CONNECTION_RETRY_INTERVAL_MS = "ORDER_FEED_WS_CONNECTION_RETRY_INTERVAL_MS";
    public static final String MID_CAP_100_NEWS_SPIKE_STOCK_LIMIT = "MID_CAP_100_NEWS_SPIKE_STOCK_LIMIT";
    public static final String SMALL_CAP_100_NEWS_SPIKE_STOCK_LIMIT = "SMALL_CAP_100_NEWS_SPIKE_STOCK_LIMIT";
    public static final String NIFTY_500_NEWS_SPIKE_STOCK_LIMIT = "NIFTY_500_NEWS_SPIKE_STOCK_LIMIT";
    public static final String NIFTY_50_NEWS_SPIKE_STOCK_LIMIT = "NIFTY_50_NEWS_SPIKE_STOCK_LIMIT";

    // Config Constant
    public static final String NEWS_SPIKE_PROFIT_PERCENT = "NEWS_SPIKE_PROFIT_PERCENT" ;
    public static final String NEWS_SPIKE_LOSS_PERCENT = "NEWS_SPIKE_LOSS_PERCENT" ;

    public static final String NEW_SPIKE_MAX_STOCKS_PER_DAY = "NEW_SPIKE_MAX_STOCKS_PER_DAY";

    public static final String KOTAK_NIFTY_BANK_INSTRUMENT = "KOTAK_NIFTY_BANK_INSTRUMENT";

    public static final String IS_SL_TRIGGERED_FLAG = "IS_SL_TRIGGERED_FLAG";

    public static final String SCALPING_TARGET_POINTS = "SCALPING_TARGET_POINTS";

    public static final String FNO_SL_MONITORING_THREAD_DURATION = "FNO_SL_MONITORING_THREAD_DURATION";// in minutes

    public static final String TRADING_NINE_TWENTY_TIME = "TRADING_NINE_TWENTY_TIME";

    public static final String TRADING_SESSION_END_TIME = "TRADING_SESSION_END_TIME";

    public static final String TRADING_SESSION_START_TIME = "TRADING_SESSION_START_TIME";

    public static final String FNO_CONSECUTIVE_SUCCESS_ORDER_COUNT = "FNO_CONSECUTIVE_SUCCESS_ORDER_COUNT";

    /*
    This config is used for setting start value of trend data average calculation. From this value we will calculate
    difference between prev and current trend value and this difference is storing on trend set.
     */
    public static final String AVG_TREND_POINTS_START_FROM = "AVG_TREND_POINTS_START_FROM";

    public static final String MAX_RETRY_PLACE_ORDER = "MAX_RETRY_PLACE_ORDER";

    public static final String FNO_TARGET_MONITORING_THREAD_DURATION = "FNO_TARGET_MONITORING_THREAD_DURATION";

    public static final String BIG_MOVE_THRESHOLD = "BIG_MOVE_THRESHOLD";
}
