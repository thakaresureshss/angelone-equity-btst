package com.trade.algotrade.client.angelone.websocket.smartTicker;

import org.json.JSONArray;

public interface SmartWSOnTicks {
	void onTicks(JSONArray ticks);
}
