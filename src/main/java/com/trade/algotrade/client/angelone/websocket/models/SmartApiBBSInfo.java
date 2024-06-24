package com.trade.algotrade.client.angelone.websocket.models;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SmartApiBBSInfo {
	public static final int BYTES = (2 * Short.BYTES) + (2 * Long.BYTES);

	// siBbBuySellFlag = 1 buy
	// siBbBuySellFlag = 0 sell
	private short buySellFlag = -1;
	private long quantity = -1;
	private long price = -1;
	private short numberOfOrders = -1;
}