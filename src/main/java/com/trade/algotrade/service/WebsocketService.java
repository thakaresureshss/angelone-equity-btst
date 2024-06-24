package com.trade.algotrade.service;

public interface WebsocketService {


    void connectOrderFeed();

    void reconnectOrderFeed();

	void disconnectAngelOneWebsocket();

	void subscribeAngelOneWebsocketMock(Long instrumentToken);

	void subscribeKotakOrderWebsocketMock(Long orderId, String UserId, String price, String buySell);

	void connectOrderFeedWebsocket();

	void onOrderStatusUpdate(String data);

	// New methods added 
	void connectWebsocket();

	void disconnectWebsocket();

	void reconnectWebsocket();

	void subscribeInstrument(Long instrumentToken);

	void connectAndSubscribeInstrument(Long instrumentToken);

	void connectAndSubscribeWatchInstrument();

	void unsubscribeInstrument(Long instrumentToken);
}
