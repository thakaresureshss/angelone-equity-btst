package com.trade.algotrade.client.angelone.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProductType {

	NORMAL("NORMAL"), INTRADAY("INTRADAY"), MIS("MIS"), CARRYFORWARD("CARRYFORWARD");

	private final String value;

	ProductType(String value) {
		this.value = value;
	}

	@JsonValue
	public String toString() {
		return this.value;
	}

	@JsonCreator
	public static ProductType fromValue(String value) {
		for (ProductType b : ProductType.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}

}
