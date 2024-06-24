package com.trade.algotrade.client.angelone.websocket.models;

import com.trade.algotrade.client.angelone.utils.ByteUtils;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

import static com.trade.algotrade.client.angelone.utils.Constants.*;


@Getter
@Setter
public class SnapQuote {
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
	private long lastTradedTimestamp = 0;
	private long openInterest = 0;
	private double openInterestChangePerc = 0;
	private SmartApiBBSInfo[] bestFiveBuy;
	private SmartApiBBSInfo[] bestFiveSell;
	private long upperCircuit = 0;
	private long lowerCircuit = 0;
	private long yearlyHighPrice = 0;
	private long yearlyLowPrice = 0;

	public SnapQuote(ByteBuffer buffer) {
        this.subscriptionMode = buffer.get(SUBSCRIPTION_MODE);
        this.token = ByteUtils.getTokenID(buffer);
        this.exchangeType = this.token.getExchangeType();
        this.sequenceNumber = buffer.getLong(SEQUENCE_NUMBER_OFFSET);
        this.exchangeFeedTimeEpochMillis = buffer.getLong(EXCHANGE_FEED_TIME_OFFSET);
        this.lastTradedPrice = buffer.getLong(LAST_TRADED_PRICE_OFFSET);
        this.lastTradedQty = buffer.getLong(LAST_TRADED_QTY_OFFSET);
        this.avgTradedPrice = buffer.getLong(AVG_TRADED_PRICE_OFFSET);
        this.volumeTradedToday = buffer.getLong(VOLUME_TRADED_TODAY_OFFSET);
        this.totalBuyQty = buffer.getDouble(TOTAL_BUY_QTY_OFFSET);
        this.totalSellQty = buffer.getDouble(TOTAL_SELL_QTY_OFFSET);
        this.openPrice = buffer.getLong(OPEN_PRICE_OFFSET);
        this.highPrice = buffer.getLong(HIGH_PRICE_OFFSET);
        this.lowPrice = buffer.getLong(LOW_PRICE_OFFSET);
        this.closePrice = buffer.getLong(CLOSE_PRICE_OFFSET);
        this.lastTradedTimestamp = buffer.getLong(LAST_TRADED_TIMESTAMP_OFFSET);
        this.openInterest = buffer.getLong(OPEN_INTEREST_OFFSET);
        this.openInterestChangePerc = buffer.getDouble(OPEN_INTEREST_CHANGE_PERC_OFFSET);
        this.bestFiveBuy = ByteUtils.getBestFiveBuyData(buffer);
        this.bestFiveSell = ByteUtils.getBestFiveSellData(buffer);
        this.upperCircuit = buffer.getLong(UPPER_CIRCUIT_OFFSET);
        this.lowerCircuit = buffer.getLong(LOWER_CIRCUIT_OFFSET);
        this.yearlyHighPrice = buffer.getLong(YEARLY_HIGH_PRICE_OFFSET);
        this.yearlyLowPrice = buffer.getLong(YEARLY_LOW_PRICE_OFFSET);
    }
}
