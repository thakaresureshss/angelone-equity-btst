package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WSFeedOrderStatus {

	FILL("FILL"), OPEN("OPEN"), EXECUTED("EXECUTED");

	private final String value;

	WSFeedOrderStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String toString() {
		return this.value;
	}

	@JsonCreator
	public static WSFeedOrderStatus fromValue(String value) {
		for (WSFeedOrderStatus b : WSFeedOrderStatus.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}

}
