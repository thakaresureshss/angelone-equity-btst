package com.trade.algotrade.client.angelone.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VarietyEnum {

	/**
	 * variety regular Regular order amo After Market Order co Cover Order ? iceberg
	 * Iceberg Order ? *
	 */

	REGULAR("REGULAR"), AMO("AMO"), NORMAL("NORMAL"), STOPLOSS("STOPLOSS");

	private final String value;

	VarietyEnum(String value) {
		this.value = value;
	}

	@JsonValue
	public String toString() {
		return this.value;
	}

	@JsonCreator
	public static VarietyEnum fromValue(String value) {
		for (VarietyEnum b : VarietyEnum.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}

}
