package com.trade.algotrade.client.kotak.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {

	/**
	 * Order Status
	 * Cancelled Order CAN, Confirmation Pending,CNRF Confirmation,Pending NEWF
	 */
	CAN("CAN"), OPN("OPN"), TRAD("TRAD"), NEWF("NEWF"), FIL("FIL"), SLO("SLO"), OPF("OPF");

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
