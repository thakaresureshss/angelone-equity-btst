package com.trade.algotrade.client.angelone.websocket.smartTicker;


import com.trade.algotrade.client.angelone.websocket.exception.SmartAPIException;

public interface SmartWSOnError {

	public void onError(Exception exception);

	public void onError(SmartAPIException smartAPIException);

	void onError(String error);
}
 