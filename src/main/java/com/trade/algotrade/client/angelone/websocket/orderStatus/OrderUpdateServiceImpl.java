package com.trade.algotrade.client.angelone.websocket.orderStatus;

import com.trade.algotrade.client.angelone.websocket.models.SmartStreamError;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.utils.TradeUtils;
import com.trade.algotrade.utils.WebsocketUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderUpdateServiceImpl implements OrderUpdateListner {

    @Autowired
    WebsocketUtils websocketUtils;

    @Override
    public void onConnected() {
        log.info("Angelone order feed connected");
        TradeUtils.isOrderFeedConnected = true;
    }

    @Override
    public void onDisconnected() {
        log.info("Angelone order feed disconnected");
        TradeUtils.isOrderFeedConnected = false;
    }

    @Override
    public void onError(SmartStreamError error) {
        log.info("Error in Angelone order feed {}", error);
    }

    @Override
    public void onPong() {

    }

    @Override
    public void onOrderUpdate(String data) {
        log.info("ORDER FEED : ANGELONE : {}", data);
        websocketUtils.onOrderStatusUpdate(data);
    }
}