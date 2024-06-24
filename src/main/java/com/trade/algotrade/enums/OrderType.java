package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderType {

	MARKET("MARKET"), LIMIT("LIMIT"), SL("STOPLOSS_LIMIT"), SLM("SLM"), TSL("TSL"), STOPLOSS_MARKET("STOPLOSS_MARKET");
	private final String value;

	OrderType(String value) {
		this.value = value;
	}

	@JsonValue
	public String toString() {
		return this.value;
	}

	@JsonCreator
	public static OrderType fromValue(String value) {
		for (OrderType b : OrderType.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}

}
