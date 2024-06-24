package com.trade.algotrade.client.angelone.websocket.ticker;

import com.trade.algotrade.client.angelone.websocket.models.*;

public interface SmartStreamListener {
	void onLTPArrival(LTP ltp);
	void onQuoteArrival(Quote quote);
	void onSnapQuoteArrival(SnapQuote snapQuote);

	void onDepthArrival(Depth depth);
	
	void onConnected();
	void onDisconnected();
	void onError(SmartStreamError error);
	void onPong();

	SmartStreamError onErrorCustom();
}