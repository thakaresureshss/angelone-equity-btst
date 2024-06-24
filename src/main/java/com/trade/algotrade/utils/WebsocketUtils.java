package com.trade.algotrade.utils;

import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.service.WebsocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * @author suresh.thakare
 */
@Component
public class WebsocketUtils {

    WebsocketService websocketService;

    @Autowired
    TradeUtils tradeUtils;

    @Autowired
    public WebsocketUtils(@Lazy WebsocketService websocketService) {
        this.websocketService = websocketService;
    }

    public WebsocketService getWebsocketService() {
        return websocketService;
    }

    private final static Logger logger = LoggerFactory.getLogger(TradeUtils.class);

    public void unsubscribeAngelToken(Long instrumentToken) {
       websocketService.unsubscribeInstrument(instrumentToken);
    }

    @Async
    public void addAndSubscribeInstrument(Long websocketInstrument) {
        logger.info("Adding and Subscribing instrument token : {}", websocketInstrument);
        try {
            CompletableFuture.runAsync(() -> {
                tradeUtils.addWebsocketInstruments(websocketInstrument, Constants.BANK_NIFTY);
            });
            if (!DateUtils.isDayWeekend(LocalDate.now()) && (DateUtils.isTradingSessionTime()) || !CommonUtils.getOffMarketHoursTestFlag()) {
                logger.info("**** Subscribing live feed websocket for instrument token : {}", websocketInstrument);
                websocketService.subscribeInstrument(websocketInstrument);
            } else {
                websocketService.subscribeAngelOneWebsocketMock(websocketInstrument);
            }

        } catch (DuplicateKeyException e) {
            logger.error("**** Reconnection Live Feed Error {} ****", e.getMessage());
        }
    }

    public void reconnectToWebsocket() {
        logger.debug("**** Reconnecting  Angel One live feed websocket ");
        try {
            websocketService.connectWebsocket();
        } catch (DuplicateKeyException e) {
            logger.error("**** Reconnection Live Feed Error {} ****", e);
        }
    }

    public void subscribeToWatchMasterInstruments() {
        websocketService.disconnectAngelOneWebsocket();
    }

    public void subscribeKotakOrderWebsocketMock(Long orderId, String userId, String price, String buySell) {
        websocketService.subscribeKotakOrderWebsocketMock(orderId, userId, price, buySell);
    }

    public void onOrderStatusUpdate(String orderFeedData){
        websocketService.onOrderStatusUpdate(orderFeedData);
    }
}