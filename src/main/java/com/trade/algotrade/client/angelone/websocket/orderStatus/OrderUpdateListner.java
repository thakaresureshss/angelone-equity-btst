package com.trade.algotrade.client.angelone.websocket.orderStatus;


import com.trade.algotrade.client.angelone.websocket.models.SmartStreamError;

public interface OrderUpdateListner {
    void onConnected();
    void onDisconnected();
    void onError(SmartStreamError error);
    void onPong();

    void onOrderUpdate(String data);
}