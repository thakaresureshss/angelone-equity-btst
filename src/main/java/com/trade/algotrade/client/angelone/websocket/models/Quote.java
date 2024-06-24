package com.trade.algotrade.client.angelone.websocket.models;

import com.trade.algotrade.client.angelone.utils.ByteUtils;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

import static com.trade.algotrade.client.angelone.utils.Constants.*;

@Getter
@Setter
public class Quote {
	private byte subscriptionMode;
	private ExchangeType exchangeType;
	private TokenID token;
	private long sequenceNumber;
	private long exchangeFeedTimeEpochMillis;
	private long lastTradedPrice;
	private long lastTradedQty;
	private long avgTradedPrice;
	private long volumeTradedToday;
	private double totalBuyQty;
	private double totalSellQty;
	private long openPrice;
	private long highPrice;
	private long lowPrice;
	private long closePrice;

	public Quote(ByteBuffer buffer) {
        this.subscriptionMode = buffer.get(SUBSCRIPTION_MODE);
        this.token = ByteUtils.getTokenID(buffer);
        this.exchangeType = this.token.getExchangeType();
        this.sequenceNumber = buffer.getLong(SEQUENCE_NUMBER_OFFSET);
        this.exchangeFeedTimeEpochMillis = buffer.getLong(EXCHANGE_FEED_TIME_OFFSET);
        this.lastTradedPrice = buffer.getLong(LAST_TRADED_PRICE_OFFSET);
        this.lastTradedQty = buffer.getLong(LAST_TRADED_QTY_OFFSET);
        this.avgTradedPrice = buffer.getLong(AVG_TRADED_PRICE_OFFSET);
        this.volumeTradedToday = buffer.getLong(VOLUME_TRADED_TODAY_OFFSET);
        this.totalBuyQty = buffer.getLong(TOTAL_BUY_QTY_OFFSET);
        this.totalSellQty = buffer.getLong(TOTAL_SELL_QTY_OFFSET);
        this.openPrice = buffer.getLong(OPEN_PRICE_OFFSET);
        this.highPrice = buffer.getLong(HIGH_PRICE_OFFSET);
        this.lowPrice = buffer.getLong(LOW_PRICE_OFFSET);
        this.closePrice = buffer.getLong(CLOSE_PRICE_OFFSET);
    }
}
