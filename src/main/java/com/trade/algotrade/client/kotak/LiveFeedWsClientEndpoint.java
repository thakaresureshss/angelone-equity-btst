package com.trade.algotrade.client.kotak;

import com.trade.algotrade.constants.CharConstant;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.constants.WebSocketConstant;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.enums.MessageSource;
import com.trade.algotrade.enums.WSLiveFeedCloseReson;
import com.trade.algotrade.service.ManualSchedulerService;
import com.trade.algotrade.service.SchedulerInstatiationService;
import com.trade.algotrade.service.WebsocketService;
import com.trade.algotrade.task.service.PeriodicManualLiveFeedService;
import com.trade.algotrade.task.service.PeriodicPingService;
import com.trade.algotrade.task.service.PeriodicWsRetryService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import jakarta.annotation.PostConstruct;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.websocket.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.stream.Collectors;

/**
 * ChatServer Client
 *
 * @author Suresh Thakare
 */
@ClientEndpoint
@NoArgsConstructor
@Component
public class LiveFeedWsClientEndpoint {

    private static Session session = null;
    private static MessageHandler messageHandler;
    private static LiveFeedWsClientEndpoint websocketClientEndpoint;
    private static final Logger logger = LoggerFactory.getLogger(LiveFeedWsClientEndpoint.class);
    private static WebsocketService websocketService;

    private static final PeriodicPingService pingService = new PeriodicPingService();
    private static final PeriodicManualLiveFeedService periodicLtpService = new PeriodicManualLiveFeedService();
    private static final PeriodicWsRetryService wsRetryService = new PeriodicWsRetryService();

    private static ManualSchedulerService manualSchedulerServiceStatic;

    private static TradeUtils tradeUtils;

    @Autowired
    ManualSchedulerService manualSchedulerService;

    @Autowired
    private SchedulerInstatiationService schedulerInstatiationService;

    @PostConstruct
    public void init() {
        manualSchedulerServiceStatic = manualSchedulerService;
    }

    public LiveFeedWsClientEndpoint(URI endpointURI, WebsocketService websocketService, TradeUtils tradeUtils) {
        LiveFeedWsClientEndpoint.tradeUtils = tradeUtils;
        if (websocketClientEndpoint == null || !websocketClientEndpoint.isSocketOpen()) {
            websocketClientEndpoint = new LiveFeedWsClientEndpoint();
            LiveFeedWsClientEndpoint.websocketService = websocketService;
            session = createWebsocketContainer(endpointURI);
        }
    }

    private static Session createWebsocketContainer(URI endpointURI) {
        try {
            ClientManager client = ClientManager.createClient();
            return client.asyncConnectToServer(websocketClientEndpoint, endpointURI).get();
        } catch (Exception e) {
            logger.debug("Exception Occured while connecting to URL {}", endpointURI.toString());
            throw new RuntimeException(e);
        }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param session the session which is opened.
     */

    @OnOpen
    public void onOpen(Session session) {
        logger.debug("opening live feed websocket");
        LiveFeedWsClientEndpoint.session = session;

        if (!isSocketOpen()) {
            logger.info("Connection is not opened returning control");
            return;
        }

        // TODO need to add condition to don't call first Time
        cancelLtpAndRetryWsConnectionJob();

        /**
         * This logic will retry download instruments if latest instruments are not present in database.
         */
        if (CollectionUtils.isEmpty(tradeUtils.getWebsocketInstruments())) {
            schedulerInstatiationService.initiateDownloadInstrument();
        }

        if (!CollectionUtils.isEmpty(tradeUtils.getWebsocketInstruments())) {
            String instrumentTokens = tradeUtils.getWebsocketInstruments().stream().map(i -> i.toString()).collect(Collectors.joining(","));
            logger.debug("Subscribing Tokens {}", instrumentTokens);
            websocketClientEndpoint.sendMessage(createMessagePayload(instrumentTokens));
            pingService.setPingInterval(WebSocketConstant.WS_DEFAULT_PING_INTERVAL, LiveFeedWsClientEndpoint.session, pingService);

        } else {
            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, WSLiveFeedCloseReson.EMPTY_INSTRUMENT.toString());
            logger.debug("Closing Connection  Reason: {}", closeReason.getReasonPhrase());
            try {
                session.close(closeReason);
            } catch (IOException e) {
                logger.error("IOException while closing connection reason: {}", e);
            }
        }
    }

    public String createMessagePayload(String instrumentTokens) {
        JSONArray parent = new JSONArray();
        parent.put("pageload");
        JSONObject inputToken = new JSONObject();
        inputToken.put("inputtoken", instrumentTokens);
        parent.put(inputToken);
        return WebSocketConstant.LIVE_FEED_MESSAGE + parent;
    }


    private void cancelLtpAndRetryWsConnectionJob() {
        logger.debug("cancelLtpAndRetryWsConnectionJob Called");
        periodicLtpService.cancelLtpMessageTask();
        wsRetryService.cancelWsRetryTask();
    }

    @Async
    public static void startLtpAndRetryWsConnectionJob() {
        wsRetryService.setRetryWsInterval(WebSocketConstant.WS_CONNECTION_RETRY_INTERVAL, wsRetryService, websocketService);
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client
     * send a message.
     *
     * @param subscription The text message
     */
    @OnMessage
    public void onMessage(String subscription) {
        if (StringUtils.isNoneBlank(subscription)) {
            if (subscription.equalsIgnoreCase(WebSocketConstant.WS_PONG_PAYLOAD)) {
                logger.debug("Live feed Pong Message {}", subscription);
                if (!DateUtils.isTradingSessionTime() && !CommonUtils.getOffMarketHoursTestFlag()) {
                    CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Market is closed,Closing websocket");
                    try {
                        LiveFeedWsClientEndpoint.session.close(closeReason);
                    } catch (IOException e) {
                        logger.error("Error while closing websocket connection {}", closeReason.getReasonPhrase());
                    }
                }
                return;
            }
            if (messageHandler != null && willDecode(subscription)) {
                messageHandler.handleMessage(decode(subscription));
            }
        }
    }

    @OnMessage
    public void onMessage(ByteBuffer bytes) {
    }

    @OnError
    public void onError(Session session, Throwable err) {
        logger.error("Websocket Client Error Session {} Error {}", session.getId(), err.getMessage());
    }

    /**
     * register message handler
     *
     * @param msgHandler
     */
    public void addMessageHandler(MessageHandler msgHandler) {
        LiveFeedWsClientEndpoint.messageHandler = msgHandler;
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
        try {
            logger.info("Sending message {} ", message);
            LiveFeedWsClientEndpoint.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            logger.error("Message sending error {}", message);
        }
    }

    /**
     * Message handler.
     *
     * @author Suresh Thakare
     */
    public static interface MessageHandler {

        public void handleMessage(LiveFeedWSMessage message);
    }

    public boolean isSocketOpen() {
        return LiveFeedWsClientEndpoint.session.isOpen();
    }

    public static void setsession(Session session) {
        LiveFeedWsClientEndpoint.session = session;
    }

    public LiveFeedWSMessage decode(String subscription) {
        logger.debug("Decoding subcription message  subscription {}", subscription);
        String data = subscription.substring(subscription.lastIndexOf(CharConstant.OPENING_SQR_BRACKET) + 1, subscription.lastIndexOf(CharConstant.CLOSING_SQR_BRACKET));
        String result = data.replaceAll("\"", "");
        String[] instrumentData = result.split(CharConstant.COMMA);
        Long instrumentToken = Long.valueOf(instrumentData[1]);
        BigDecimal ltp = new BigDecimal(instrumentData[6]);
        BigDecimal previousDayClose = new BigDecimal(instrumentData[10]);
        String instrumentName = instrumentData[23];
        String timeStamp = instrumentData[19];
        BigDecimal changePercent = new BigDecimal(instrumentData[20]);
        return LiveFeedWSMessage.builder().instrumentToken(instrumentToken).ltp(ltp).previousDayClose(previousDayClose).instrumentName(instrumentName).changePercent(changePercent).timeStamp(timeStamp)
                .messageSource(MessageSource.WEBSOCKET).build();
    }

    public boolean willDecode(String subscription) {
        logger.debug("Validating live feed message can be decoded or not {}", subscription);
        if (StringUtils.isBlank(subscription)) {
            return false;
        }
        boolean isDataPresent = subscription.contains(Constants.MESSAGE_GET_DATA);

        if (!isDataPresent) {
            return false;
        }

        int startIndex = subscription.lastIndexOf(CharConstant.OPENING_SQR_BRACKET);
        int endIndex = subscription.indexOf(CharConstant.CLOSING_SQR_BRACKET);
        if (startIndex < 0 || endIndex < 0) {
            return false;
        }
        String data = subscription.substring(startIndex + 1, endIndex);

        if (StringUtils.isBlank(data)) {
            return false;
        }
        String[] instrumentData = data.split(CharConstant.COMMA);
        boolean allDataPresent = instrumentData.length == 25;
        return allDataPresent;
    }

    public static Session getSession() {
        return session;
    }


}
