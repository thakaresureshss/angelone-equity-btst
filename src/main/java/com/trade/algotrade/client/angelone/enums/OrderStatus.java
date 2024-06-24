package com.trade.algotrade.client.angelone.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {

	/**
	 * Order Status
	 * Cancelled Order CAN, Confirmation Pending,CNRF Confirmation,Pending NEWF
	 */
	CAN("cancelled"), OPN("open"), TRAD("TRAD"), NEWF("NEWF"), FIL("complete"), SLO("SLO"), OPF("OPF"), OPP("open pending"),
	RJT("rejected"), TRP("trigger pending"), MDP("modify pending");

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
