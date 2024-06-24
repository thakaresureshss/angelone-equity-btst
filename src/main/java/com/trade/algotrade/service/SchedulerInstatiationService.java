package com.trade.algotrade.service;

/**
 * @author Rahul Pansare
 *
 */
public interface SchedulerInstatiationService {

	public void initiateDownloadInstrument();
	
	public void initiateFetchHolidays();

    void clearApplicationStaticData();

	void squareOffIntraDayPositions();

	void loadMarginLocally();

    void moveCompletedTradesToHistoryDocuments();

	public void initiateAngelOneLogin();

	void connectAndSubscribe();

	void disconnectAndSubscribeAngelOneWebsocket();

	void placeNewsSpikeTrades();

	void subscribeAngelOneWebsocketMock();

	void eodProcess();

	void connectAndSubscribeToken(long instrumentToken);

	void unsubscribeTestTokenInAngelWebsocket(long instrumentToken);

	void initiateOpenInterest();

	void initiateReconnectAngelOneWebsocket();
}
