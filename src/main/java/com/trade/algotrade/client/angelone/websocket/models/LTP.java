package com.trade.algotrade.client.angelone.websocket.models;

import com.trade.algotrade.client.angelone.utils.ByteUtils;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

import static com.trade.algotrade.client.angelone.utils.Constants.*;


@Getter
@Setter
public class LTP {
	private byte subscriptionMode;
	private ExchangeType exchangeType;
	private TokenID token;
	private long sequenceNumber;
	private long exchangeFeedTimeEpochMillis;
	private long lastTradedPrice;

	public LTP(ByteBuffer buffer) {
        this.subscriptionMode = buffer.get(SUBSCRIPTION_MODE);
		this.token = ByteUtils.getTokenID(buffer);
		this.exchangeType = this.token.getExchangeType();
        this.sequenceNumber = buffer.getLong(SEQUENCE_NUMBER_OFFSET);
        this.exchangeFeedTimeEpochMillis = buffer.getLong(EXCHANGE_FEED_TIME_OFFSET);
        this.lastTradedPrice = buffer.getLong(LAST_TRADED_PRICE_OFFSET);
    }
}
