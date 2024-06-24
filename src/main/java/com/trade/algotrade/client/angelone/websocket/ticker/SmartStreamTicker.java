package com.trade.algotrade.client.angelone.websocket.ticker;

import com.neovisionaries.ws.client.*;
import com.trade.algotrade.client.angelone.utils.ByteUtils;
import com.trade.algotrade.client.angelone.utils.Routes;
import com.trade.algotrade.client.angelone.utils.Utils;
import com.trade.algotrade.client.angelone.websocket.exception.SmartAPIException;
import com.trade.algotrade.client.angelone.websocket.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@Component
public class SmartStreamTicker {

    private static int pingIntervalInMilliSeconds = 10000; // 10 seconds

    private static int delayInMilliSeconds = 5000; // initial delay in seconds
    private static int periodInMilliSeconds = 5000; // initial period in seconds
    private static final String clientIdHeader = "x-client-code";
    private static final String feedTokenHeader = "x-feed-token";
    private static final String clientLibHeader = "x-client-lib";

    private final Routes routes = new Routes();
    private final String wsuri = routes.getSmartStreamWSURI();

    private SmartStreamListener smartStreamListener;
    private static WebSocket ws;
    private String clientId;
    private String feedToken;
    private EnumMap<SmartStreamSubsMode, Set<TokenID>> tokensByModeMap = new EnumMap<>(SmartStreamSubsMode.class);
    private Timer pingTimer;
    private static LocalDateTime lastPongReceivedTime = LocalDateTime.now();

    private static SmartStreamTicker smartStreamTicker = null;


    private final Logger logger = LoggerFactory.getLogger(SmartStreamTicker.class);

    public SmartStreamTicker() {

    }

    /**
     * Initializes the SmartStreamTicker.
     *
     * @param clientId            - the client ID used for authentication
     * @param feedToken           - the feed token used for authentication
     * @param smartStreamListener - the SmartStreamListener for receiving callbacks
     * @throws IllegalArgumentException - if the clientId, feedToken, or SmartStreamListener is null or empty
     */
    public SmartStreamTicker(String clientId, String feedToken, SmartStreamListener smartStreamListener) {
        if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(feedToken) || Utils.validateInputNullCheck(smartStreamListener)) {
            throw new IllegalArgumentException(
                    "clientId, feedToken and SmartStreamListener should not be empty or null");
        }

        this.clientId = clientId;
        this.feedToken = feedToken;
        this.smartStreamListener = smartStreamListener;
        init();
    }

    public static SmartStreamTicker getSmartTicker(String clientId, String feedToken, SmartStreamListener smartStreamListener) {
        if (Objects.isNull(smartStreamTicker)) {
            smartStreamTicker = new SmartStreamTicker(clientId, feedToken, smartStreamListener);
        }
        smartStreamTicker.lastPongReceivedTime = LocalDateTime.now();
        return smartStreamTicker;
    }

//    /**
//     * Initializes the SmartStreamTicker.
//     *
//     * @param clientId            - the client ID used for authentication
//     * @param feedToken           - the feed token used for authentication
//     * @param delay               - delay in milliseconds
//     * @param period              - period in milliseconds
//     * @param smartStreamListener - the SmartStreamListener for receiving callbacks
//     * @throws IllegalArgumentException - if the clientId, feedToken, or SmartStreamListener is null or empty
//     */
//    public SmartStreamTicker(String clientId, String feedToken, SmartStreamListener smartStreamListener, Integer delay, Integer period) {
//        if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(feedToken) || Utils.isEmpty(delay) || Utils.isEmpty(period) || Utils.validateInputNullCheck(smartStreamListener)) {
//            throw new IllegalArgumentException(
//                    "clientId, feedToken and SmartStreamListener should not be empty or null");
//        }
//        this.delayInMilliSeconds = delay;
//        this.periodInMilliSeconds = period;
//        this.clientId = clientId;
//        this.feedToken = feedToken;
//        this.smartStreamListener = smartStreamListener;
//        init();
//    }


    private void init() {
        try {
            ws = new WebSocketFactory()
                    .setVerifyHostname(false)
                    .createSocket(wsuri)
                    .setPingInterval(pingIntervalInMilliSeconds);
            ws.addHeader(clientIdHeader, clientId);
            ws.addHeader(feedTokenHeader, feedToken);
            ws.addHeader(clientLibHeader, "JAVA");
            ws.addListener(getWebsocketAdapter());
        } catch (IOException e) {
            if (Utils.validateInputNotNullCheck(smartStreamListener)) {
                log.info("Websocket init failed.");
                smartStreamListener.onError(getErrorHolder(e));
            }
        }
    }


    private SmartStreamError getErrorHolder(Throwable e) {
        SmartStreamError error = new SmartStreamError();
        error.setException(e);
        return error;
    }

    /**
     * Returns a WebSocketAdapter to listen to ticker related events.
     */
    public WebSocketAdapter getWebsocketAdapter() {
        return new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws WebSocketException {
                smartStreamListener.onConnected();
                startPingTimer(websocket);
            }

            @Override
            public void onTextMessage(WebSocket websocket, String message) throws Exception {
                super.onTextMessage(websocket, message);
            }

            @Override
            public void onBinaryMessage(WebSocket websocket, byte[] binary) {
                SmartStreamSubsMode mode = SmartStreamSubsMode.findByVal(binary[0]);
                if (Utils.validateInputNullCheck(mode)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid SubsMode=");
                    sb.append(binary[0]);
                    sb.append(" in the response binary packet");
                    smartStreamListener.onError(getErrorHolder(new SmartAPIException(sb.toString())));
                }
                try {
                    switch (mode) {
                        case LTP: {
                            ByteBuffer packet = ByteBuffer.wrap(binary).order(ByteOrder.LITTLE_ENDIAN);
                            LTP ltp = ByteUtils.mapToLTP(packet);
                            smartStreamListener.onLTPArrival(ltp);
                            break;
                        }
                        case QUOTE: {
                            ByteBuffer packet = ByteBuffer.wrap(binary).order(ByteOrder.LITTLE_ENDIAN);
                            Quote quote = ByteUtils.mapToQuote(packet);
                            smartStreamListener.onQuoteArrival(quote);
                            break;
                        }
                        case SNAP_QUOTE: {
                            ByteBuffer packet = ByteBuffer.wrap(binary).order(ByteOrder.LITTLE_ENDIAN);
                            SnapQuote snapQuote = ByteUtils.mapToSnapQuote(packet);
                            smartStreamListener.onSnapQuoteArrival(snapQuote);
                            break;
                        }
                        case DEPTH_20: {
                            ByteBuffer packet = ByteBuffer.wrap(binary).order(ByteOrder.LITTLE_ENDIAN);
                            Depth depth = ByteUtils.mapToDepth20(packet);
                            smartStreamListener.onDepthArrival(depth);
                            break;
                        }
                        default: {
                            smartStreamListener.onError(getErrorHolder(
                                    new SmartAPIException("SubsMode=" + mode + " in the response is not handled.")));
                            break;
                        }
                    }
                } catch (Exception e) {
                    //smartStreamListener.onError(getErrorHolder(e));
                    log.error("Smart ticker processing error {}", e.getStackTrace());
                    log.error("Smart ticker processing Exception Occurred {}", e.getMessage());

                }
            }

            @Override
            public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                try {
                    SmartStreamTicker.lastPongReceivedTime = LocalDateTime.now();
                    smartStreamListener.onPong();
                } catch (Exception e) {
                    SmartStreamError error = new SmartStreamError();
                    error.setException(e);
                    smartStreamListener.onError(error);
                }
            }

            /**
             * On disconnection, return statement ensures that the thread ends.
             *
             * @param websocket
             * @param serverCloseFrame
             * @param clientCloseFrame
             * @param closedByServer
             * @throws Exception
             */
            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
                                       WebSocketFrame clientCloseFrame, boolean closedByServer) {
                log.info("************* [onDisconnected] closedByServer = {}", closedByServer);
                try {
                    if (closedByServer) {
                        reconnectAndSubscribe();
                    } else {
                        stopPingTimer();
                        smartStreamListener.onDisconnected();
                    }
                } catch (Exception e) {
                    SmartStreamError error = new SmartStreamError();
                    error.setException(e);
                    smartStreamListener.onError(error);
                }
            }

            @Override
            public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                super.onCloseFrame(websocket, frame);
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                smartStreamListener.onErrorCustom();
            }
        };
    }

    private void startPingTimer(final WebSocket websocket) {

        pingTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    LocalDateTime currentTime = LocalDateTime.now();

                    if (lastPongReceivedTime.isBefore(currentTime.minusSeconds(90))) {
                        log.info("****************** NO RESPONSE FROM SERVER, RECONNECTING AGAIN ****************");
                        websocket.disconnect();
                        connectAndSubscribe();
                    }
                } catch (Exception e) {
                    smartStreamListener.onError(getErrorHolder(e));
                }
            }
        };
        pingTimer.scheduleAtFixedRate(timerTask, delayInMilliSeconds, periodInMilliSeconds);// run at every 5 second
    }

    private void stopPingTimer() {
        if (Utils.validateInputNotNullCheck(pingTimer)) {
            pingTimer.cancel();
            pingTimer = null;
        }
    }

    private void connectWebsocket() throws WebSocketException {
        smartStreamTicker = SmartStreamTicker.getSmartTicker(this.clientId, this.feedToken, this.smartStreamListener);
        smartStreamTicker.lastPongReceivedTime = LocalDateTime.now();
        if (Objects.nonNull(smartStreamTicker)) {
            connect();
        }
    }

    private void connectAndSubscribe() throws WebSocketException {
        log.debug("connectAndSubscribe - started");
        // resubscribing the existing tokens as per the mode
        connectWebsocket();
        tokensByModeMap.forEach((mode, tokens) -> {
            subscribe(mode, tokens);
        });
        log.info("connectAndSubscribe - done");
    }

    private void reconnectAndSubscribe() throws WebSocketException {
        log.info("reconnectAndSubscribe - started");
        // resubscribing the existing tokens as per the mode
        smartStreamTicker.disconnect();
        connectWebsocket();
        tokensByModeMap.forEach((mode, tokens) -> {
            subscribe(mode, tokens);
        });
        log.info("reconnectAndSubscribe - done");
    }

    /**
     * Disconnects websocket connection.
     */
    public void disconnect() {

        if (ws != null) {
            stopPingTimer();
            ws.disconnect();
        }
    }

    /**
     * Returns true if websocket connection is open.
     *
     * @return boolean
     */
    public boolean isConnectionOpen() {
        return (ws != null) && ws.isOpen();
    }

    /**
     * Returns true if websocket connection is closed.
     *
     * @return boolean
     */
    public boolean isConnectionClosed() {
        return !isConnectionOpen();
    }

    /**
     * Subscribes tokens.
     */
    public void subscribe(SmartStreamSubsMode mode, Set<TokenID> tokens) {
        if (ws != null && ws.isOpen()) {
            for (TokenID token : tokens) {
                if (ExchangeType.NSE_CM.equals(token.getExchangeType()) && SmartStreamSubsMode.DEPTH_20.equals(mode)) {
                    if (tokens.size() < 50) {
                        JSONObject wsMWJSONRequest = getApiRequest(SmartStreamAction.SUBS, mode, tokens);
                        ws.sendText(wsMWJSONRequest.toString());
                        tokensByModeMap.put(mode, tokens);
                    } else {
                        smartStreamListener.onError(getErrorHolder(new SmartAPIException("Token size should be less than 50", "504")));
                    }
                } else {
                    log.info("**** Subscribing Bank NIFTY Angel one instrument : {}", token.getToken());
                    if (!ExchangeType.NSE_CM.equals(token.getExchangeType()) && SmartStreamSubsMode.DEPTH_20.equals(mode)) {
                        smartStreamListener.onError(getErrorHolder(new SmartAPIException("Invalid Exchange Type: Please check the exchange type and try again", "504")));
                    } else {
                        JSONObject wsMWJSONRequest = getApiRequest(SmartStreamAction.SUBS, mode, tokens);
                        ws.sendText(wsMWJSONRequest.toString());
                        tokensByModeMap.put(mode, tokens);
                    }
                }
            }
        } else {
            smartStreamListener.onError(getErrorHolder(new SmartAPIException("ticker is null not connected", "504")));
        }
    }

    /**
     * Unsubscribes tokens.
     */
    public void unsubscribe(SmartStreamSubsMode mode, Set<TokenID> tokens) {
        logger.info("UnSubscribing Tokens {} for Mode {}", tokens, mode);
        if (ws != null) {
            if (ws.isOpen()) {
                JSONObject wsMWJSONRequest = getApiRequest(SmartStreamAction.UNSUBS, mode, tokens);
                ws.sendText(wsMWJSONRequest.toString());
                Set<TokenID> currentlySubscribedTokens = tokensByModeMap.get(mode);
                if (currentlySubscribedTokens != null) {
                    currentlySubscribedTokens.removeAll(tokens);
                }
            } else {
                logger.error("Websocket Connection is not Open Couldn't Unsubscribe Tokens {} ", tokens);
                smartStreamListener.onError(getErrorHolder(new SmartAPIException("ticker is not connected", "504")));
            }
        } else {
            logger.error("Websocket is not initialized Tokens {} ", tokens);
            smartStreamListener.onError(getErrorHolder(new SmartAPIException("ticker is null not connected", "504")));
        }
    }

    private JSONArray generateExchangeTokensList(Set<TokenID> tokens) {
        Map<ExchangeType, JSONArray> tokensByExchange = new EnumMap<>(ExchangeType.class);
        tokens.stream().forEach(t -> {
            JSONArray tokenList = tokensByExchange.get(t.getExchangeType());
            if (tokenList == null) {
                tokenList = new JSONArray();
                tokensByExchange.put(t.getExchangeType(), tokenList);
            }

            tokenList.put(t.getToken());
        });

        JSONArray exchangeTokenList = new JSONArray();
        tokensByExchange.forEach((ex, t) -> {
            JSONObject exchangeTokenObj = new JSONObject();
            exchangeTokenObj.put("exchangeType", ex.getVal());
            exchangeTokenObj.put("tokens", t);

            exchangeTokenList.put(exchangeTokenObj);
        });

        return exchangeTokenList;
    }

    private JSONObject getApiRequest(SmartStreamAction action, SmartStreamSubsMode mode, Set<TokenID> tokens) {
        JSONObject params = new JSONObject();
        params.put("mode", mode.getVal());
        params.put("tokenList", this.generateExchangeTokensList(tokens));

        JSONObject wsMWJSONRequest = new JSONObject();
        wsMWJSONRequest.put("action", action.getVal());
        wsMWJSONRequest.put("params", params);

        return wsMWJSONRequest;
    }

    public void connect() throws WebSocketException {
        synchronized (this) {
            try {
                log.info("CURRENT STATE OF WEBSOCKET: {}, HashCode of SmartTicker {}", smartStreamTicker.ws.getState(), smartStreamTicker.hashCode());
                if (Objects.isNull(smartStreamTicker.ws) || Objects.isNull(smartStreamTicker.ws.getState()) || !smartStreamTicker.ws.getState().equals(WebSocketState.CREATED)) {
                    if (smartStreamTicker.ws.getState().equals(WebSocketState.OPEN)) {
                        log.debug("CURRENT STATE OF WEBSOCKET: {}, Hence Closing Existing Websocket Connection", smartStreamTicker.ws.getState());
                        smartStreamTicker.ws.disconnect();
                    }
                    if (!smartStreamTicker.ws.getState().equals(WebSocketState.CONNECTING)) {
                        init();
                    }
                }
                log.debug("BEFORE CONNECT STATE OF WEBSOCKET: {}", smartStreamTicker.ws.getState());
                if (!smartStreamTicker.ws.getState().equals(WebSocketState.CONNECTING)) {
                    ws.connect();
                }
                log.debug("connected to uri: {}", wsuri);
            } catch (Exception exception) {
                log.error("Exception in smart websocket connection {} ", exception.getMessage());

            }
        }
    }

}
