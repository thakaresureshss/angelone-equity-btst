package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TradeStatus {

	CANCELLED("CANCELLED"), OPEN("OPEN"), EXECUTED("EXECUTED"), COMPLETED("COMPLETED"), REJECTED("REJECTED");

	private final String value;

	TradeStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String toString() {
		return this.value;
	}

	@JsonCreator
	public static TradeStatus fromValue(String value) {
		for (TradeStatus b : TradeStatus.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}

}
