package com.trade.algotrade.service.impl;

import com.neovisionaries.ws.client.WebSocketException;
import com.trade.algotrade.client.angelone.AngelOneClient;
import com.trade.algotrade.client.angelone.enums.OrderStatus;
import com.trade.algotrade.client.angelone.response.feed.OrderUpdateData;
import com.trade.algotrade.client.angelone.response.feed.OrderUpdateResponse;
import com.trade.algotrade.client.angelone.response.feed.QouteResponse;
import com.trade.algotrade.client.angelone.response.user.GetAccessTokenResponse;
import com.trade.algotrade.client.angelone.websocket.models.ExchangeType;
import com.trade.algotrade.client.angelone.websocket.models.SmartStreamSubsMode;
import com.trade.algotrade.client.angelone.websocket.models.TokenID;
import com.trade.algotrade.client.angelone.websocket.orderStatus.OrderUpdateListner;
import com.trade.algotrade.client.angelone.websocket.orderStatus.OrderUpdateWebsocket;
import com.trade.algotrade.client.angelone.websocket.ticker.SmartStreamListener;
import com.trade.algotrade.client.angelone.websocket.ticker.SmartStreamTicker;
import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.OrderFeedWsClientEndpoint;
import com.trade.algotrade.client.kotak.response.WebsocketAccessTokenResponse;
import com.trade.algotrade.constants.*;
import com.trade.algotrade.dto.OrderMessage;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.dto.websocket.OrderFeedWSMessage;
import com.trade.algotrade.enums.MessageSource;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.exceptions.BusinessError;
import com.trade.algotrade.repo.InstrumentMasterRepo;
import com.trade.algotrade.repo.InstrumentWatchMasterRepo;
import com.trade.algotrade.service.*;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.JsonUtils;
import com.trade.algotrade.utils.TradeUtils;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author suresh.thakare
 */

@Service
@NoArgsConstructor
public class WebScoketServiceImpl implements WebsocketService {

    private final Logger logger = LoggerFactory.getLogger(WebScoketServiceImpl.class);

    @Autowired
    KotakClient kotakClient;

    @Autowired
    AngelOneClient angelOneClient;

    @Autowired
    JsonUtils jsonutils;

    @Autowired
    StrategyBuilderService strategyBuilderService;

    @Autowired
    EquityStrategyBuilderService equityStrategyBuilderService;

    @Autowired
    TradeUtils tradeUtils;


    @Autowired
    ManualSchedulerService manualSchedulerService;

    @Autowired
    SmartStreamListener smartStreamListener;

    @Autowired
    SmartStreamTicker smartStreamTicker;

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    InstrumentMasterRepo instrumentMasterRepo;

    @Autowired
    OrderFeedWsClientEndpoint clientEndpoint;

    @Autowired
    InstrumentService instrumentService;

    @Autowired
    OrderUpdateListner orderUpdateListner;

    @Autowired
    InstrumentWatchMasterRepo instrumentWatchMasterRepo;


    private List<WebsocketAccessTokenResponse> connectActiveUserToOrderFeedWs() {
        logger.debug(" ****** [connectActiveUserToOrderFeedWs ] Called ********");
        List<WebsocketAccessTokenResponse> allActiveUserOrderFeedTokens = null;
        try {
            allActiveUserOrderFeedTokens = kotakClient.getAllActiveUserOrderWebsocketToken();
            if (CollectionUtils.isEmpty(allActiveUserOrderFeedTokens) || allActiveUserOrderFeedTokens.stream().allMatch(at -> StringUtils.isBlank(at.getResult().getToken()))) {
                throw new AlgotradeException(ErrorCodeConstants.EMPTY_NULL_OR_SESSION_WEBSOCKET_TOKEN_ERROR);
            }
        } catch (Exception e) {
            logger.info("****[connectActiveUserToOrderFeedWs] Authentication Exception {}", e.getMessage());
            throw e;
        }

        List<WebsocketAccessTokenResponse> activeUsers = allActiveUserOrderFeedTokens.stream().filter(at -> StringUtils.isNoneBlank(at.getResult().getToken())).collect(Collectors.toList());
        logger.debug(" ****** [connectActiveUserToOrderFeedWs ] Completed for {} USERS ********", activeUsers.size());
        return activeUsers;
    }


    @Override
    public void connectOrderFeed() {
        List<WebsocketAccessTokenResponse> websocketAccessTokenResponses = connectActiveUserToOrderFeedWs();
        if (!CollectionUtils.isEmpty(websocketAccessTokenResponses)) {
            List<CompletableFuture<Void>> allFutures = websocketAccessTokenResponses.stream().map(user -> {
                return getOrderFeedWsObject(user).thenAcceptAsync(websocketSession -> {
                    listenOrderFeedMessage(websocketSession, user.getUserId());
                });
            }).collect(Collectors.toList());
            // This is added because execution need to be hold here for it
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));
            logger.info("Order feed websocket connected for all the active user ");
            logger.info(" ****** [connectOrderFeedWebsocket ] Completed ********");
        }
    }

    @Override
    public void connectOrderFeedWebsocket() {
        logger.info("Get token and Connect order feed websocket called ");
        GetAccessTokenResponse websocketSystemUser = angelOneClient.getAccessTokenSystemUser();
        OrderUpdateWebsocket orderUpdateWebsocket = new OrderUpdateWebsocket(websocketSystemUser.getData().getJwtToken(), orderUpdateListner);
        try {
            orderUpdateWebsocket.connect();
        } catch (WebSocketException e) {
            logger.info("Get token and Connect order feed websocket Completed with Exception {}", e.getMessage());
            throw new AlgotradeException(BusinessError.builder().status(HttpStatus.BAD_REQUEST).message("Exception in connect Angel one order feed").build());
        }
        logger.info("Get token and Connect order feed websocket Completed ");
    }

    @Override
    public void onOrderStatusUpdate(String data) {
        OrderUpdateResponse orderUpdateResponse = jsonutils.fromJson(data, OrderUpdateResponse.class);
        logger.debug("Mapping order feed object :: {}", orderUpdateResponse);
        OrderFeedWSMessage message = OrderFeedWSMessage.builder()
                .orderId(orderUpdateResponse.getOrderData().getOrderId())
                .userId(orderUpdateResponse.getUserId())
                .price(String.valueOf(orderUpdateResponse.getOrderData().getAverageprice()))
                .quantity(orderUpdateResponse.getOrderData().getQuantity())
                .buySell(orderUpdateResponse.getOrderData().getTransactionType())
                .status(orderUpdateResponse.getOrderData().getOrderStatus())
                .variety(orderUpdateResponse.getOrderData().getVariety())
                .orderTag(orderUpdateResponse.getOrderData().getOrdertag())
                .build();
        processOrderMessage(message);
    }

    public CompletableFuture<OrderFeedWsClientEndpoint> getOrderFeedWsObject(WebsocketAccessTokenResponse
                                                                                     orderFeedUser) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Getting order feed session for User ID {}", orderFeedUser.getUserId());
            try {
                return clientEndpoint.getWebsocketClientEndpoint(new URI(WebSocketConstant.ORDER_FEED_URL
                        .concat("?EIO=3&transport=websocket&access_token=" + orderFeedUser.getResult().getToken())), jsonutils, this);
            } catch (URISyntaxException e) {
                logger.error("Exception Occurred while getting order feed session forUser ID  {}", orderFeedUser.getUserId());
                throw new RuntimeException(e);
            }
        });
    }

    public void
    listenOrderFeedMessage(OrderFeedWsClientEndpoint orderFeedClient, String userId) {
        logger.debug("Added message handler for User ID {} = Session ID {}", userId, orderFeedClient.getOrderSession().getId());
        orderFeedClient.addMessageHandler(message -> {
            try {
                processOrderMessage(message);
            } catch (Exception e) {
                logger.error("Exception in process order ::{}", e.getMessage());
            }
            //equityStrategyBuilderService.monitorEqutiyOrders(message);
        });
    }

    @Override
    public void reconnectOrderFeed() {
        logger.debug(" ****** [reconnectOrderFeed ] Called  ********");
        Session userSession = clientEndpoint.getSession();
        try {
            CloseReason closeReason = new CloseReason(CloseCodes.NORMAL_CLOSURE, "Restarting connection");
            userSession.close(closeReason);
            try {
                if (CommonUtils.getWebsocketOnFlag()) {
                    connectOrderFeed();
                }
            } catch (Exception e) {
                logger.error("Exception occurred reconnecting Order Feed WebSocket connection:: {}", e.getMessage());
            }
            logger.debug(" ****** [reconnectOrderFeed ] Completed  ********");
        } catch (IOException e) {
            logger.error("IOException occurred while closing order WebSocket connection: {}", e.getMessage());
        }
    }

    @Override
    public void disconnectAngelOneWebsocket() {
        if (smartStreamTicker.isConnectionOpen()) {
            logger.debug("************* disconnecting existing websocket called ********************");
            smartStreamTicker.disconnect();
        }
    }

    /**
     * This Method will get executed after order is placed. The instrument tokens used for the orders will be used
     * to  fetch the ltp details.
     */


    //@Async
    private void processOrderMessage(OrderFeedWSMessage orderFeedWSMessage) {
        synchronized (this) {
            if (strategyBuilderService.ignoreActionOnOrderFeedStatues(orderFeedWSMessage)) {
                logger.debug("USER ID {} ORDER ID {} --> ORDER FEED --> NO ACTION REQUIRED -> for the Order Status {} , Message Source {}", orderFeedWSMessage.getUserId(), orderFeedWSMessage.getOrderId(), orderFeedWSMessage.getStatus(), orderFeedWSMessage.getMessageSource());
                return;
            }
            validateAndInsertOrderFeedToQueue(orderFeedWSMessage);
        }
        // Added this synchronized block to handle duplicate order feed insertion in queue.
    }

    private void validateAndInsertOrderFeedToQueue(OrderFeedWSMessage orderFeedWSMessage) {

        if (Objects.isNull(orderFeedWSMessage) || !Constants.ALGOTRADE_TAG.equalsIgnoreCase(orderFeedWSMessage.getOrderTag())) {
            logger.info("NON SYSTEM GENERATED ORDER ORDER FEED > received Hence returning from order feed queue flow  USER ID {} ORDER ID {}, Order Status {}, Transaction Type {}", orderFeedWSMessage.getUserId(), orderFeedWSMessage.getOrderId(), orderFeedWSMessage.getStatus(), orderFeedWSMessage.getBuySell());
            return;
        }
        if (!TradeUtils.orderFeedSet.add(new OrderMessage(orderFeedWSMessage.getStatus(), orderFeedWSMessage.getOrderId()))) {
            logger.info("DUPLICATE ORDER FEED > received Hence returning from order feed queue flow for USER ID {} ORDER ID {}, Order Status {}, Transaction Type {}", orderFeedWSMessage.getUserId(), orderFeedWSMessage.getOrderId(), orderFeedWSMessage.getStatus(), orderFeedWSMessage.getBuySell());
            return;
        }
        logger.info("UNIQUE ORDER FEED - received for USER ID {} ORDER ID {}, Order Status {}, Transaction Type {}", orderFeedWSMessage.getUserId(), orderFeedWSMessage.getOrderId(), orderFeedWSMessage.getStatus(), orderFeedWSMessage.getBuySell());
        TradeUtils.orderFeedQueue.add(orderFeedWSMessage);

        com.trade.algotrade.client.angelone.enums.OrderStatus orderStatus = OrderStatus.fromValue(orderFeedWSMessage.getStatus());
        // This case will happen only SL order Executed at broker.
        if ("SELL".equalsIgnoreCase(orderFeedWSMessage.getBuySell()) && "NORMAL".equalsIgnoreCase(orderFeedWSMessage.getVariety()) && orderStatus == com.trade.algotrade.client.angelone.enums.OrderStatus.FIL) {
            strategyBuilderService.processSLOrderFeed(orderFeedWSMessage);
        }
    }


    @Override
    public void subscribeAngelOneWebsocketMock(Long order) {
        ExchangeType exchangetype = ExchangeType.NSE_CM;
        long kotakInstrumentToken = order;
        if (CommonUtils.getOffMarketHoursTestFlag()) {
            BigDecimal basePrice;
            if (kotakInstrumentToken == 99926009L) {
                basePrice = new BigDecimal(commonUtils.getConfigValue(ConfigConstants.MOCK_BANK_NIFTY_CURRENT_VALUE));
            } else {
                QouteResponse ltp = angelOneClient.getQuote(com.trade.algotrade.client.angelone.enums.ExchangeType.NSE, List.of(String.valueOf(kotakInstrumentToken)));
                if (ltp != null && ltp.getData() != null) {
                    TradeUtils.indiaVixStrike = ltp.getData().getFetched().stream().findFirst().get().getLtp();
                }
//                LtpResponse ltp = kotakClient.getLtp(String.valueOf(kotakInstrumentToken));
//                Optional<LtpSuccess> ltpOptional = ltp.getSuccess().stream().findFirst();
//                if (ltpOptional.isEmpty()) {
//                    basePrice = new BigDecimal(150);
//                } else {
////                    basePrice = ltpOptional.get().getLastPrice();
//                    basePrice = new BigDecimal(150);
//                }
                basePrice = new BigDecimal(150);
                exchangetype = ExchangeType.NSE_FO;
            }
            if (runWebscoket(exchangetype, kotakInstrumentToken, basePrice)) return;
        }
    }

    @Override
    public void subscribeKotakOrderWebsocketMock(Long orderId, String userId, String price, String buySell) {
        List<String> orderFeedList = new ArrayList<>();
        if ("B".equalsIgnoreCase(buySell)) {
            orderFeedList.add("{\"user-id\": \"R54391496\",\"status-code\": \"200\",\"order-status\": \"AB09\",\"error-message\": \"\",\"orderData\": {\"variety\": \"NORMAL\",\"ordertype\": \"MARKET\",\"ordertag\": \"AlgotradeTag\",\"producttype\": \"INTRADAY\",\"price\": 0.0,\"triggerprice\": 0.0,\"quantity\": \"15\",\"disclosedquantity\": \"0\",\"duration\": \"DAY\",\"squareoff\": 0.0,\"stoploss\": 0.0,\"trailingstoploss\": 0.0,\"tradingsymbol\": \"BANKNIFTY29MAY2449600CE\",\"transactiontype\": \"BUY\",\"exchange\": \"NFO\",\"symboltoken\": \"56669\",\"instrumenttype\": \"OPTIDX\",\"strikeprice\": 49600.0,\"optiontype\": \"CE\",\"expirydate\": \"29MAY2024\",\"lotsize\": \"15\",\"cancelsize\": \"0\",\"averageprice\": 0.0,\"filledshares\": \"0\",\"unfilledshares\": \"15\",\"orderid\": \"240527000921678\",\"text\": \"\",\"status\": \"open pending\",\"orderstatus\": \"open pending\",\"updatetime\": \"27-May-2024 14:21:24\",\"exchtime\": \"\",\"exchorderupdatetime\": \"\",\"fillid\": \"\",\"filltime\": \"\",\"parentorderid\": \"\"}}\"\"}}");
            orderFeedList.add("{\"user-id\": \"R54391496\",\"status-code\": \"200\",\"order-status\": \"AB09\",\"error-message\": \"\",\"orderData\": {\"variety\": \"NORMAL\",\"ordertype\": \"MARKET\",\"ordertag\": \"AlgotradeTag\",\"producttype\": \"INTRADAY\",\"price\": 0.0,\"triggerprice\": 0.0,\"quantity\": \"15\",\"disclosedquantity\": \"0\",\"duration\": \"DAY\",\"squareoff\": 0.0,\"stoploss\": 0.0,\"trailingstoploss\": 0.0,\"tradingsymbol\": \"BANKNIFTY29MAY2449600CE\",\"transactiontype\": \"BUY\",\"exchange\": \"NFO\",\"symboltoken\": \"56669\",\"instrumenttype\": \"OPTIDX\",\"strikeprice\": 49600.0,\"optiontype\": \"CE\",\"expirydate\": \"29MAY2024\",\"lotsize\": \"15\",\"cancelsize\": \"0\",\"averageprice\": 0.0,\"filledshares\": \"0\",\"unfilledshares\": \"15\",\"orderid\": \"240527000921678\",\"text\": \"\",\"status\": \"open\",\"orderstatus\": \"open\",\"updatetime\": \"27-May-2024 14:21:24\",\"exchtime\": \"\",\"exchorderupdatetime\": \"\",\"fillid\": \"\",\"filltime\": \"\",\"parentorderid\": \"\"}}\"\"}}");
            orderFeedList.add("{\"user-id\": \"R54391496\",\"status-code\": \"200\",\"order-status\": \"AB09\",\"error-message\": \"\",\"orderData\": {\"variety\": \"NORMAL\",\"ordertype\": \"MARKET\",\"ordertag\": \"AlgotradeTag\",\"producttype\": \"INTRADAY\",\"price\": 0.0,\"triggerprice\": 0.0,\"quantity\": \"15\",\"disclosedquantity\": \"0\",\"duration\": \"DAY\",\"squareoff\": 0.0,\"stoploss\": 0.0,\"trailingstoploss\": 0.0,\"tradingsymbol\": \"BANKNIFTY29MAY2449600CE\",\"transactiontype\": \"BUY\",\"exchange\": \"NFO\",\"symboltoken\": \"56669\",\"instrumenttype\": \"OPTIDX\",\"strikeprice\": 49600.0,\"optiontype\": \"CE\",\"expirydate\": \"29MAY2024\",\"lotsize\": \"15\",\"cancelsize\": \"0\",\"averageprice\": 200.0,\"filledshares\": \"0\",\"unfilledshares\": \"15\",\"orderid\": \"240527000921678\",\"text\": \"\",\"status\": \"complete\",\"orderstatus\": \"complete\",\"updatetime\": \"27-May-2024 14:21:24\",\"exchtime\": \"\",\"exchorderupdatetime\": \"\",\"fillid\": \"\",\"filltime\": \"\",\"parentorderid\": \"\"}}\"\"}}");
        } else if ("TRP".equalsIgnoreCase(buySell)) {
            orderFeedList.add("{\"user-id\": \"R54391496\",\"status-code\": \"200\",\"order-status\": \"AB09\",\"error-message\": \"\",\"orderData\": {\"variety\": \"STOPLOSS\",\"ordertype\": \"MARKET\",\"ordertag\": \"AlgotradeTag\",\"producttype\": \"INTRADAY\",\"price\": 0.0,\"triggerprice\": 0.0,\"quantity\": \"15\",\"disclosedquantity\": \"0\",\"duration\": \"DAY\",\"squareoff\": 0.0,\"stoploss\": 0.0,\"trailingstoploss\": 0.0,\"tradingsymbol\": \"BANKNIFTY29MAY2449600CE\",\"transactiontype\": \"BUY\",\"exchange\": \"NFO\",\"symboltoken\": \"56669\",\"instrumenttype\": \"OPTIDX\",\"strikeprice\": 49600.0,\"optiontype\": \"CE\",\"expirydate\": \"29MAY2024\",\"lotsize\": \"15\",\"cancelsize\": \"0\",\"averageprice\": 200.0,\"filledshares\": \"0\",\"unfilledshares\": \"15\",\"orderid\": \"240527000921678\",\"text\": \"\",\"status\": \"open pending\",\"orderstatus\": \"open pending\",\"updatetime\": \"27-May-2024 14:21:24\",\"exchtime\": \"\",\"exchorderupdatetime\": \"\",\"fillid\": \"\",\"filltime\": \"\",\"parentorderid\": \"\"}}\"\"}}");
            orderFeedList.add("{\"user-id\": \"R54391496\",\"status-code\": \"200\",\"order-status\": \"AB09\",\"error-message\": \"\",\"orderData\": {\"variety\": \"STOPLOSS\",\"ordertype\": \"MARKET\",\"ordertag\": \"AlgotradeTag\",\"producttype\": \"INTRADAY\",\"price\": 0.0,\"triggerprice\": 0.0,\"quantity\": \"15\",\"disclosedquantity\": \"0\",\"duration\": \"DAY\",\"squareoff\": 0.0,\"stoploss\": 0.0,\"trailingstoploss\": 0.0,\"tradingsymbol\": \"BANKNIFTY29MAY2449600CE\",\"transactiontype\": \"BUY\",\"exchange\": \"NFO\",\"symboltoken\": \"56669\",\"instrumenttype\": \"OPTIDX\",\"strikeprice\": 49600.0,\"optiontype\": \"CE\",\"expirydate\": \"29MAY2024\",\"lotsize\": \"15\",\"cancelsize\": \"0\",\"averageprice\": 200.0,\"filledshares\": \"0\",\"unfilledshares\": \"15\",\"orderid\": \"240527000921678\",\"text\": \"\",\"status\": \"trigger pending\",\"orderstatus\": \"trigger pending\",\"updatetime\": \"27-May-2024 14:21:24\",\"exchtime\": \"\",\"exchorderupdatetime\": \"\",\"fillid\": \"\",\"filltime\": \"\",\"parentorderid\": \"\"}}\"\"}}");
        } else if ("S".equalsIgnoreCase(buySell)) {
            orderFeedList.add("{\"user-id\": \"R54391496\",\"status-code\": \"200\",\"order-status\": \"AB09\",\"error-message\": \"\",\"orderData\": {\"variety\": \"NORMAL\",\"ordertype\": \"MARKET\",\"ordertag\": \"AlgotradeTag\",\"producttype\": \"INTRADAY\",\"price\": 0.0,\"triggerprice\": 0.0,\"quantity\": \"15\",\"disclosedquantity\": \"0\",\"duration\": \"DAY\",\"squareoff\": 0.0,\"stoploss\": 0.0,\"trailingstoploss\": 0.0,\"tradingsymbol\": \"BANKNIFTY29MAY2449600CE\",\"transactiontype\": \"BUY\",\"exchange\": \"NFO\",\"symboltoken\": \"56669\",\"instrumenttype\": \"OPTIDX\",\"strikeprice\": 49600.0,\"optiontype\": \"CE\",\"expirydate\": \"29MAY2024\",\"lotsize\": \"15\",\"cancelsize\": \"0\",\"averageprice\": 200.0,\"filledshares\": \"0\",\"unfilledshares\": \"15\",\"orderid\": \"240527000921678\",\"text\": \"\",\"status\": \"complete\",\"orderstatus\": \"complete\",\"updatetime\": \"27-May-2024 14:21:24\",\"exchtime\": \"\",\"exchorderupdatetime\": \"\",\"fillid\": \"\",\"filltime\": \"\",\"parentorderid\": \"\"}}\"\"}}");
        }
        orderFeedList.forEach(feed -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            JsonUtils utils = new JsonUtils();
            OrderUpdateResponse orderUpdateResponse = jsonutils.fromJson(feed, OrderUpdateResponse.class);
            OrderUpdateData orderData = orderUpdateResponse.getOrderData();
            OrderFeedWSMessage orderFeedWSMessage = OrderFeedWSMessage.builder()
                    .orderId(String.valueOf(orderId))
                    .userId(userId)
                    .price(price)
                    .avgPrice(price)
                    .buySell(buySell)
                    .quantity(orderData.getQuantity())
                    .messageSource(MessageSource.WEBSOCKET)
                    .status(orderData.getStatus())
                    .orderTag(orderData.getOrdertag())
                    .variety(orderData.getVariety())
                    .build();
            processOrderMessage(orderFeedWSMessage);
        });
    }

    public OrderFeedWSMessage decode(String orderMessage) {
        logger.info("ORDER FEED WEBSOCKET ORDER STATUS UPDATE MESSAGE {}", orderMessage);
        int startIndex = orderMessage.lastIndexOf(CharConstant.OPENING_SQR_BRACKET);
        int endIndex = orderMessage.indexOf(CharConstant.CLOSING_SQR_BRACKET);
        String data = orderMessage.substring(startIndex + 1, endIndex);
        String orderMessages = data.substring(data.indexOf(CharConstant.COMMA) + 1);
        OrderFeedWSMessage wsOrderMessage = jsonutils.fromJson(orderMessages, OrderFeedWSMessage.class);
        wsOrderMessage.setMessageSource(MessageSource.WEBSOCKET);
        return wsOrderMessage;
    }

    private boolean runWebscoket(ExchangeType exchangetype, long kotakInstrumentToken, BigDecimal basePrice) {
        logger.info("Running mock websocket");
        BigDecimal initialValue = basePrice;
        while (!CommonUtils.pauseMockWebsocket) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            BigDecimal mockTick = getMockTick().setScale(2, RoundingMode.CEILING);
            Optional<Long> optionalLong = instrumentWatchMasterRepo.findAll().stream().filter(entity -> entity.getInstrumentToken().equals(kotakInstrumentToken)).map(kotakInstrumentMasterEntity -> kotakInstrumentMasterEntity.getInstrumentToken()).findFirst();
            if (optionalLong.isEmpty()) {
                logger.debug("**** Active instruments not present hence returning");
                return true;
            }
            basePrice = basePrice.add(mockTick);
            //Test SL order to execute on price drop.
            if (exchangetype.equals(ExchangeType.NSE_FO) && (basePrice.subtract(initialValue)).compareTo(new BigDecimal(40)) > 0) {
                basePrice = basePrice.subtract(new BigDecimal(12));
            }
            LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(kotakInstrumentToken).ltp(basePrice).instrumentName(exchangetype.equals(ExchangeType.NSE_CM) ? Constants.BANK_NIFTY_INDEX : Constants.BANK_NIFTY).timeStamp(DateUtils.epochTimeToString(System.currentTimeMillis())).messageSource(MessageSource.WEBSOCKET).build();
            logger.info("Running mock websocket for feed {}", liveFeedWSMessage);
            manualSchedulerService.processLtpMessage(liveFeedWSMessage);
        }
        return false;
    }

    private static BigDecimal getMockTick() {
        int min = -8;//TODO move to DB
        int max = 2;
        return new BigDecimal((Math.random() * (max - min + 1) + min));
    }

    // TODO Latest method
    @Override
    public void connectWebsocket() {
        GetAccessTokenResponse websocketSystemUser = angelOneClient.getAccessTokenSystemUser();
        if (Objects.nonNull(smartStreamTicker) && smartStreamTicker.isConnectionClosed() && Objects.nonNull(websocketSystemUser)) {
            smartStreamTicker = SmartStreamTicker.getSmartTicker(WebSocketConstant.ANGEL_ONE_CLIENT, websocketSystemUser.getData().getFeedToken(), smartStreamListener);
            try {
                smartStreamTicker.connect();
            } catch (WebSocketException e) {
                logger.error(" ****** Connecting to websocket for instrument token failed due to {}", e.getMessage());
            }
            logger.info("Connecting to websocket completed.");
        } else {
            logger.info("Failed to connect websocket.");
        }
    }

    @Override
    public void disconnectWebsocket() {
        if (Objects.nonNull(smartStreamTicker) && smartStreamTicker.isConnectionOpen()) {
            logger.info("Disconnecting existing websocket called.");
            smartStreamTicker.disconnect();
        }
    }

    @Override
    public void reconnectWebsocket() {
        logger.info("Reconnecting websocket.");
        disconnectWebsocket();
        connectWebsocket();
    }

    @Override
    public void subscribeInstrument(Long instrumentToken) {
        Set<TokenID> tokens = new HashSet<>();
        ExchangeType exchangeType = ExchangeType.NSE_FO;
        TokenID tokenId = new TokenID(exchangeType, instrumentToken.toString());
        tokens.add(tokenId);

        if (Objects.nonNull(smartStreamTicker) && smartStreamTicker.isConnectionOpen()) {
            logger.info("Websocket is Open, Subscribing websocket instrument {}", tokens.size());
            smartStreamTicker.subscribe(SmartStreamSubsMode.LTP, tokens);
        } else {
            logger.info("Websocket is not open Hence, Re-Connect websocket instrument {}", tokens.size());
            connectAndSubscribeInstrument(instrumentToken);
        }

        logger.debug("Subscribing tokens to websocket completes ********************");
    }

    @Override
    public void connectAndSubscribeInstrument(Long instrumentToken) {
        connectWebsocket();

        Set<TokenID> tokens = new HashSet<>();
        ExchangeType exchangeType = ExchangeType.NSE_FO;
        TokenID tokenId = new TokenID(exchangeType, instrumentToken.toString());
        tokens.add(tokenId);
        smartStreamTicker.subscribe(SmartStreamSubsMode.LTP, tokens);
        logger.info("Subscribing to websocket for {} token ********************", instrumentToken);
    }

    @Override
    public void connectAndSubscribeWatchInstrument() {
        connectWebsocket();

        Set<TokenID> tokens = new HashSet<>();
        Map<Long, String> websocketInstrumentMap = tradeUtils.getWebsocketALLInstruments();
        if (CollectionUtils.isEmpty(websocketInstrumentMap)) {
            logger.info("TOKEN NOT FOUND TOKEN WATCHLIST, Hence Feed won't received");
        } else {
            for (Map.Entry<Long, String> instrumentWatch : websocketInstrumentMap.entrySet()) {
                ExchangeType exchangeType = Constants.BANK_NIFTY_INDEX.equalsIgnoreCase(instrumentWatch.getValue()) ? ExchangeType.NSE_CM : ExchangeType.NSE_FO;
                Long aLong = instrumentWatch.getKey();
                TokenID tokenId = new TokenID(exchangeType, aLong.toString());
                tokens.add(tokenId);
            }
        }

        logger.info("Subscribing to websocket for {} tokens ********************", tokens.size());

        if (!CollectionUtils.isEmpty(tokens)) {
            smartStreamTicker.subscribe(SmartStreamSubsMode.LTP, tokens);
        }
        logger.info("Subscribing to websocket completes.");
    }

    @Override
    public void unsubscribeInstrument(Long instrumentToken) {
        logger.info("Unsubscribe instrument token {} from websocket.", instrumentToken);
        Set<TokenID> tokens = new HashSet<>();
        TokenID tokenId = new TokenID(ExchangeType.NSE_FO, instrumentToken.toString());
        tokens.add(tokenId);
        if (Objects.nonNull(smartStreamTicker) && smartStreamTicker.isConnectionOpen()) {
            logger.debug("Websocket connection is open,Hence unsubscribing token {}", instrumentToken);
            smartStreamTicker.unsubscribe(SmartStreamSubsMode.LTP, tokens);
        }
    }
}