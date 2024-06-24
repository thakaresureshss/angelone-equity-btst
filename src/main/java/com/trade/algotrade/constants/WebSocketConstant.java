
package com.trade.algotrade.constants;

import org.springframework.stereotype.Component;

@Component
public class WebSocketConstant {
	public static final Integer WS_CONNECTION_TIMEOUT_DEFAULT = 0;
	public static final String WS_PING_PAYLOAD = "2";
	public static final String WS_PONG_PAYLOAD = "3";
	public static final String LIVE_FEED_MESSAGE = "42";
	public static final Integer WS_DEFAULT_PING_INTERVAL = 24500;
	public static final Integer MAX_RETRY_WEBSOCKET_ACCESS_TOKEN = 3;
	public static final Integer MAX_RETRY_LTP = 3;
	public static String LIVE_FEED_WS_URL = "wss://wstreamer.kotaksecurities.com/feed/";
	public static String ORDER_FEED_URL = "wss://wstreamer.kotaksecurities.com/feed/orders/";
	public static final Integer WS_DEFAULT_LTP_INTERVAL = 500;
	public static final Integer WS_CONNECTION_RETRY_INTERVAL = 120000;

	public static final String ANGEL_ONE_CLIENT = "R54391496";
}
