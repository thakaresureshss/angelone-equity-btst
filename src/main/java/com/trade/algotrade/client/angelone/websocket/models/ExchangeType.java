package com.trade.algotrade.client.angelone.websocket.models;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public enum ExchangeType {
	NSE_CM(1), NSE_FO(2), BSE_CM(3), BSE_FO(4), MCX_FO(5), NCX_FO(7), CDE_FO(13);

	private int val;
	private static final Map<Integer, ExchangeType> valToExchangeTypeMap = new HashMap<>();

	private ExchangeType(int val) {
		this.val = val;
	}
	
	static {
		valToExchangeTypeMap.put(1, NSE_CM);
		valToExchangeTypeMap.put(2, NSE_FO);
		valToExchangeTypeMap.put(3, BSE_CM);
		valToExchangeTypeMap.put(4, BSE_FO);
		valToExchangeTypeMap.put(5, MCX_FO);
		valToExchangeTypeMap.put(7, NCX_FO);
		valToExchangeTypeMap.put(13, CDE_FO);
	}

	public int getVal() {
		return this.val;
	}
	
	public static ExchangeType findByValue(int val) {
		ExchangeType exchange = valToExchangeTypeMap.get(val);
		if (exchange == null) {
			throw new NoSuchElementException(String.format("No ExchangeType found with value: %s", val));
		}
		return exchange;
	}
}
