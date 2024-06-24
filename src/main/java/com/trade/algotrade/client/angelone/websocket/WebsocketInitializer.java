package com.trade.algotrade.client.angelone.websocket;

import com.trade.algotrade.client.angelone.AngelOneClientImpl;
import com.trade.algotrade.client.angelone.websocket.exception.SmartAPIException;
import com.trade.algotrade.client.angelone.websocket.smartTicker.*;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebsocketInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AngelOneClientImpl.class);

    public void smartWebSocketUsage(String clientId, String jwtToken, String apiKey, String actionType, String feedToken)
            throws SmartAPIException {

        SmartWebsocket smartWebsocket = new SmartWebsocket(clientId, jwtToken, apiKey, actionType, feedToken);

        smartWebsocket.setOnConnectedListener(new SmartWSOnConnect() {

            @Override
            public void onConnected() {

                smartWebsocket.runscript();
            }
        });

        smartWebsocket.setOnDisconnectedListener(new SmartWSOnDisconnect() {
            @Override
            public void onDisconnected() {
                logger.info("onDisconnected");
            }
        });

        /** Set error listener to listen to errors. */
        smartWebsocket.setOnErrorListener(new SmartWSOnError() {
            @Override
            public void onError(Exception exception) {
                logger.info("onError: " + exception.getMessage());
            }

            @Override
            public void onError(SmartAPIException smartAPIException) {
                logger.info("onError: " + smartAPIException.getMessage());
            }

            @Override
            public void onError(String error) {
                logger.info("onError: " + error);
            }
        });

        smartWebsocket.setOnTickerArrivalListener(new SmartWSOnTicks() {
            @Override
            public void onTicks(JSONArray ticks) {
                logger.info("ticker data: " + ticks.toString());
            }
        });

        /**
         * connects to Smart API ticker server for getting live quotes
         */
        smartWebsocket.connect();

        /**
         * You can check, if websocket connection is open or not using the following
         * method.
         */
        boolean isConnected = smartWebsocket.isConnectionOpen();
        logger.info("is connected {}", isConnected);

        // After using SmartAPI ticker, close websocket connection.
        // smartWebsocket.disconnect();

    }

}
