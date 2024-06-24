package com.trade.algotrade.client.kotak;

import com.trade.algotrade.constants.CharConstant;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.constants.WebSocketConstant;
import com.trade.algotrade.dto.websocket.OrderFeedWSMessage;
import com.trade.algotrade.enums.MessageSource;
import com.trade.algotrade.service.ManualSchedulerService;
import com.trade.algotrade.service.WebsocketService;
import com.trade.algotrade.service.impl.WebScoketServiceImpl;
import com.trade.algotrade.task.service.PeriodicOrderMonitoringService;
import com.trade.algotrade.task.service.PeriodicOrderWsRetryService;
import com.trade.algotrade.task.service.PeriodicPingService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.JsonUtils;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.tyrus.client.ClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;


/**
 * ChatServer Client
 *
 * @author Suresh Thakare
 */
@ClientEndpoint
@Component
public class OrderFeedWsClientEndpoint {
    private Session session;
    private MessageHandler messageHandler;
    private PeriodicPingService pingService;
    private PeriodicOrderWsRetryService wsRetryService = new PeriodicOrderWsRetryService();
    static JsonUtils jsonUtils;

    private static WebsocketService websocketService;

    private static final Logger logger = LoggerFactory.getLogger(OrderFeedWsClientEndpoint.class);


    @Autowired
    ManualSchedulerService manualSchedulerService;

    static ManualSchedulerService manualSchedulerServiceStatic;

    private static final PeriodicOrderMonitoringService periodicOrderMonitoringService = new PeriodicOrderMonitoringService();


    @PostConstruct

    public void init() {
        manualSchedulerServiceStatic = manualSchedulerService;
    }

    public OrderFeedWsClientEndpoint getWebsocketClientEndpoint(URI endpointURI, JsonUtils jsonUtils,
                                                                WebScoketServiceImpl websocketService) {
        OrderFeedWsClientEndpoint orderFeedWsClientEndpoint = new OrderFeedWsClientEndpoint();
        this.session = createWebsocketContainer(orderFeedWsClientEndpoint, endpointURI);
        orderFeedWsClientEndpoint.jsonUtils = jsonUtils;
        orderFeedWsClientEndpoint.websocketService = (WebsocketService) websocketService;
        return orderFeedWsClientEndpoint;
    }

    private Session createWebsocketContainer(OrderFeedWsClientEndpoint websocketClientEndpoint, URI endpointURI) {
        try {
            ClientManager client = ClientManager.createClient();
            return client.connectToServer(websocketClientEndpoint, endpointURI);
        } catch (Exception e) {
            logger.error("Websocket Order Feed Connection Error {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param websocket the session which is opened.
     */

    @OnOpen
    public void onOpen(Session websocket) {
        logger.info("Opening Order Websocket Session {}", websocket.getId());
        this.session = websocket;
        jsonUtils = new JsonUtils();
        pingService = new PeriodicPingService();
        Integer pingTimeInterval = WebSocketConstant.WS_DEFAULT_PING_INTERVAL;
        pingService.setOrderFeedPingInterval(pingTimeInterval, this.session,
                pingService);
        String orderFeedWebsocketRetryCancelReason = "Order websocket connected";
        cancelOrderFeedWebsocketRetry(orderFeedWebsocketRetryCancelReason, websocket);
        logger.debug("Opening Order Websocket Session {}", websocket.getId());
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param session the session which is getting closed.
     * @param reason  the reason for connection close
     */
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        logger.info("closing order feed websocket for session {} Reason {}", session.getId(), reason.getReasonPhrase());
        if ((DateUtils.isTradingSessionTime() || CommonUtils.getOffMarketHoursTestFlag()) && !session.isOpen()) {
            startOrderAndRetryWsConnectionJob();
        }
    }

    private void cancelOrderFeedWebsocketRetry(String reason, Session websocket) {
        logger.info("Cancelling Order Feed Websocket retry Reason :  {} for User ID : {}", reason, websocket.getId());
        wsRetryService.cancelWsRetryTask(reason, websocket);
    }

    public void startOrderAndRetryWsConnectionJob() {

        int orderWebsocketRetryInterval = WebSocketConstant.WS_CONNECTION_RETRY_INTERVAL;
        if (CommonUtils.getOffMarketHoursTestFlag()) {
            orderWebsocketRetryInterval = 5000;
        }
        wsRetryService.setRetryOrderWsInterval(this, orderWebsocketRetryInterval, wsRetryService,
                websocketService);
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client
     * send a message.
     *
     * @param orderMessage The text message
     */
    @OnMessage
    public void onMessage(String orderMessage) {
        if (StringUtils.isNoneBlank(orderMessage) && orderMessage.equalsIgnoreCase(WebSocketConstant.WS_PONG_PAYLOAD)) {
            handlePongMessage(orderMessage);
        } else if (StringUtils.isNoneBlank(orderMessage) && messageHandler != null && willDecode(orderMessage)) {
            handleOrderFeedMessage(orderMessage);
        }
    }

    private void handleOrderFeedMessage(String orderMessage) {
        logger.debug("Handling order feed message for Session ID {} and Message {}", this.getOrderSession().getId(), orderMessage);
        messageHandler.handleMessage(decode(orderMessage));
    }

    private void handlePongMessage(String orderMessage) {
        logger.debug("Order feed Pong Message received for session  {} and message {}", this.getOrderSession().getId(), orderMessage);
        if (!DateUtils.isTradingSessionTime() && !CommonUtils.getOffMarketHoursTestFlag()) {
            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE,
                    "Market is closed,Closing websocket");
            try {
                this.session.close(closeReason);
            } catch (IOException e) {
                logger.error("Error while closing websocket session {} connection Reason {}", this.getOrderSession().getId(), closeReason.getReasonPhrase());
            }
        }
        return;
    }

    @OnError
    public void onError(Session session, Throwable err) {
        logger.error("Order feed websocket client Error Session {} Error {}", session.getId(), err.getMessage());
    }

    /**
     * register message handler
     */
    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    /**
     * Send a message.
     */
    public void sendMessage(String message) {
        this.session.getAsyncRemote().sendText(message);
    }

    /**
     * Message handler.
     *
     * @author Suresh Thakare
     */
    public static interface MessageHandler {

        public void handleMessage(OrderFeedWSMessage orderFeedWSMessage);

    }

    public Session getSession() {
        return this.session;
    }


    public OrderFeedWSMessage decode(String orderMessage) {
        logger.info("ORDER FEED WEBSOCKET ORDER STATUS UPDATE MESSAGE {}", orderMessage);
        int startIndex = orderMessage.lastIndexOf(CharConstant.OPENING_SQR_BRACKET);
        int endIndex = orderMessage.indexOf(CharConstant.CLOSING_SQR_BRACKET);
        String data = orderMessage.substring(startIndex + 1, endIndex);
        String orderMessages = data.substring(data.indexOf(CharConstant.COMMA) + 1);
        OrderFeedWSMessage wsOrderMessage = jsonUtils.fromJson(orderMessages, OrderFeedWSMessage.class);
        wsOrderMessage.setMessageSource(MessageSource.WEBSOCKET);
        return wsOrderMessage;
    }

    public boolean willDecode(String orderMessage) {
        logger.debug("Validating order feed message can be decoded or not {}", orderMessage);
        if (StringUtils.isBlank(orderMessage)) {
            return false;
        }
        boolean isDataPresent = orderMessage.contains(Constants.MESSAGE_GET_DATA);
        if (!isDataPresent) {
            return false;
        }
        int startIndex = orderMessage.lastIndexOf(CharConstant.OPENING_SQR_BRACKET);
        int endIndex = orderMessage.indexOf(CharConstant.CLOSING_SQR_BRACKET);
        if (startIndex < 0 || endIndex < 0) {
            return false;
        }
        String data = orderMessage.substring(startIndex + 1, endIndex);
        if (StringUtils.isBlank(data)) {
            return false;
        }
        String orderMessages = data.substring(data.indexOf(CharConstant.COMMA) + 1);

        if (StringUtils.isBlank(orderMessages)) {
            return false;
        }
        return jsonUtils.isValidJson(orderMessages, OrderFeedWSMessage.class);
    }

    public Session getOrderSession() {
        return this.session;
    }


    public static void initiateOrderMonitoringScheduler() {
        long interval = WebSocketConstant.WS_DEFAULT_LTP_INTERVAL;
        if (CommonUtils.getOffMarketHoursTestFlag()) {
            interval = 15000;
        }
        periodicOrderMonitoringService.setMonitorOrderInterval(interval, manualSchedulerServiceStatic);
    }

    public static void stopOrderMonitoringScheduler() {
        logger.info("**** Stopping MANUAL ORDER MONITORING JOB(OrderMonitoringTask). ****");
        periodicOrderMonitoringService.cancelOrderMonitoringTask();
        logger.info("**** Stopped MANUAL ORDER MONITORING JOB(OrderMonitoringTask). ****");
    }
}
