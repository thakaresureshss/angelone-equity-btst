package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {

	CANCELLED("CANCELLED"), OPEN("OPEN"), EXECUTED("EXECUTED"), REJECTED("REJECTED");

	private final String value;

	OrderStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String toString() {
		return this.value;
	}

	@JsonCreator
	public static OrderStatus fromValue(String value) {
		for (OrderStatus b : OrderStatus.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}

}
