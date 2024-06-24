package com.trade.algotrade.service.impl;

import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.dto.KotakOrderDto;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;
import com.trade.algotrade.client.kotak.response.KotakGetAllOrderResponse;
import com.trade.algotrade.client.kotak.response.LtpResponse;
import com.trade.algotrade.constants.CharConstant;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.dto.websocket.OrderFeedWSMessage;
import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.*;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author suresh.thakare
 */

@Service
@NoArgsConstructor
public class ManualSchedulerServiceImpl implements ManualSchedulerService {

    private final Logger logger = LoggerFactory.getLogger(ManualSchedulerServiceImpl.class);

    @Autowired
    KotakClient kotakClient;

    @Autowired
    CandleService candleService;

    @Autowired
    StrategyBuilderService strategyBuilderService;

    @Autowired
    EquityStrategyBuilderService equityStrategyBuilderService;

    @Autowired
    UserService userService;

    @Autowired
    TradeUtils tradeUtils;

    @Autowired
    OrderService orderService;

    @Autowired
    InstrumentService instrumentService;

    @Autowired
    CommonUtils commonUtils;

    // @Async
    @Override
    public void processLtpMessage(LiveFeedWSMessage message) {
        logger.debug("Processing Message {}", message);
        if (Constants.BANK_NIFTY_INDEX.equalsIgnoreCase(message.getInstrumentName())) {
            if (indexInstrumentTickMessage(message)) return;
        } else {
            orderInstrumentTickMessage(message);
        }
        logger.debug("Processing Message Completed {}", message);
    }

    private void orderInstrumentTickMessage(LiveFeedWSMessage message) {
        logger.debug("**** Non Nifty Bank Index Tick Received, Instrument Token {} & LTP {} ,Message Source {}", message.getInstrumentToken(),
                message.getLtp(), message.getMessageSource());

        synchronized (this) {
            strategyBuilderService.monitorPositions(message);
        }
        if (DateUtils.isTimeBetween(DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)), DateUtils.getPostFiveMinuteMarketOpenTime())) {
           // equityStrategyBuilderService.buildEquityStrategy(message, Strategy.EQUITY_TOP_MOVERS);
        }
    }

    private boolean indexInstrumentTickMessage(LiveFeedWSMessage message) {
        logger.debug("Start generating Candle and initiating BIG CANDLE strategy");
        candleService.buildCandleBankNiftyCandles(message.getInstrumentName(), message.getLtp());
        return processWebsocketFeed(message);
    }

    private boolean processWebsocketFeed(LiveFeedWSMessage message) {
        if(TradeUtils.isLatestTrendFinderActive) {
            if (!commonUtils.isTodayExpiryDay() && (DateUtils.isBeforeTime(commonUtils.getConfigValue(ConfigConstants.BIG_CANDLE_TRADING_START_TIME)) || DateUtils.isAfterTime(commonUtils.getConfigValue(ConfigConstants.BIG_CANDLE_TRADING_END_TIME)))) {
                // Don't trade before 10 AM and After 3.15 PM, in the morning
                logger.debug("Returning from Big Candle processing flow Because,before 10 AM or After 3.15 PM no trade allowed for strategy := {} *****", Strategy.BIGCANDLE);
                return true;
            }
            if (userService.getAllActiveSegmentEnabledUsers(Segment.FNO).stream().allMatch(a -> StringUtils.isNoneBlank(TradeUtils.getUserDayLimitExceeded().get(a.getUserId())))) {
                logger.info("Returning from Big Candle processing flow Because, All Users are reached Daily Limit := {} *****", Strategy.BIGCANDLE);
                /// This condition is added to avoid Unwanted process BigCandle Orders. Ultimately those are rejected but to avoid resource utilization this condition added.
                //TODO check day loss and profit also and return if it exceed than defined limit.
                return true;
            }
            strategyBuilderService.buildBigCandleStrategy(message, Strategy.BIGCANDLE);
            return false;
        }
        logger.debug("TREND FINDER IS NOT ACTIVE ", Strategy.BIGCANDLE);
        return false;
    }

    //@Async
    private void processLtpSuccess(LtpSuccess ltpSuccess, String instrumentTokens) {
        logger.debug("**** [liveFeedManualScheduler ] LTP Found for Token {} LTP {}", instrumentTokens, ltpSuccess.getLastPrice());
        LiveFeedWSMessage message = LiveFeedWSMessage.builder().instrumentToken(ltpSuccess.getInstrumentToken())
                .instrumentName(ltpSuccess.getInstrumentName()).ltp(ltpSuccess.getLastPrice())
                .messageSource(MessageSource.MANULJOB)
                .build();
        logger.info("**** [liveFeedManualScheduler ] Processing LTP Message for Instrument Token := {}, LTP :={},Message Source:= {}", instrumentTokens, ltpSuccess.getLastPrice(), message.getMessageSource());
        processLtpMessage(message);
    }
/*

    @Override
    // @Async
    public void orderFeedManualScheduler() {

        userService.getAllActiveUsers().stream()
                .filter(userResponse1 -> BrokerEnum.KOTAK_SECURITIES.toString().equalsIgnoreCase(userResponse1.getBroker()))
                .forEach(user -> {
                    List<OpenOrderEntity> todaysOpenOrderByUserId = orderService.getTodaysOpenOrdersByUserId(user.getUserId());
                    if (CollectionUtils.isEmpty(todaysOpenOrderByUserId)) {
                        logger.info("No open orders found for the USER ID := {}", user.getUserId());
                        return;
                    }
                    logger.info(" ****** [orderFeedManualScheduler ] called for User ID := {}", user.getUserId());
                    if (user.getIsRealTradingEnabled()) {
                        logger.info(" ****** [orderFeedManualScheduler ] KOTAK Order Status Update : Started ********");
                        // If flag is true means real order can be placed to Kotak
                        updateSLAndOrderStatusOnKotakOrder(user, todaysOpenOrderByUserId);
                    } else {
                        logger.debug(" ****** [orderFeedManualScheduler ] Algo trade DB Open  Order Status Update : Started ********");
                        List<OpenOrderEntity> openTradesByUser = orderService.getTodaysOpenOrdersByUserId(user.getUserId());

                        if (CollectionUtils.isEmpty(openTradesByUser)) {
                            logger.info("No open orders found for the user := {}", user.getUserId());
                            return;
                        }

                        openTradesByUser.stream()
                                .filter(order -> OrderCategoryEnum.NEW == order.getOrderCategory() || (OrderCategoryEnum.SQAREOFF == order.getOrderCategory() && OrderType.MARKET == order.getOrderType()))
                                .forEach(order -> {
                                    OrderFeedWSMessage message = OrderFeedWSMessage.builder()
                                            .instrumentToken(order.getInstrumentToken())
                                            .quantity(String.valueOf(15))
//                                    .price(String.valueOf(order.getSlDetails().getTriggerPrice()))
                                            .userId(user.getUserId())
                                            .messageSource(MessageSource.MANULJOB)
                                            .status(OrderStatusWebsocket.FIL.toString())
                                            .orderId(String.valueOf(order.getOrderId()))
                                            .build();
//                            if (order.getOrderType() == OrderType.MARKET && OrderCategoryEnum.NEW == order.getOrderCategory()) {
                                    updatePriceFirstMarketOrderPlaced(order, message);
//                            }
                                    logger.debug(" ****** [orderFeedManualScheduler ] Initiating process order message ********");
                                    processOrderMessage(message);
                                });
                    }
                });

        List<OpenOrderEntity> allOpenOrders = orderService.getTodaysOrdersByStatus(OrderStatus.OPEN);
    }
*/

    private void updatePriceFirstMarketOrderPlaced(OpenOrderEntity order, OrderFeedWSMessage message) {
        logger.debug(" ****** [updatePriceFirstMarketOrderPlaced ] Called for ORDER ID := {} and USER ID := {}, Message Source := {} LTP:= {}", order.getOrderId(), message.getUserId(), message.getMessageSource(), message.getPrice());
        LtpResponse ltp = kotakClient.getLtp(String.valueOf(order.getInstrumentToken()));
        Optional<LtpSuccess> ltpOptional = ltp.getSuccess().stream().findFirst();
        if (ltpOptional.isEmpty()) {
            return;
        }
        message.setPrice(String.valueOf(ltpOptional.get().getLastPrice()));
        logger.info(" ****** [updatePriceFirstMarketOrderPlaced ] Order Execution Price := {} set for ORDER ID := {},", message.getPrice(), order.getOrderId());

//        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) == 0) {
//            LtpResponse ltp;
//            ltp = kotakClient.getLtp(String.valueOf(order.getInstrumentToken()));
//            if (Objects.nonNull(ltp) && !CollectionUtils.isEmpty(ltp.getSuccess())) {
//                Optional<LtpSuccess> ltpOptional = ltp.getSuccess().stream().findFirst();
//                if (BigDecimal.ZERO.compareTo(ltpOptional.get().getLastPrice()) == 0) {
//                    LtpSuccess ltpSuccess = new LtpSuccess();
//                    if (OrderCategoryEnum.NEW == order.getOrderCategory()) {
//                        ltpSuccess.lastPrice = new BigDecimal(100);
//                    } else {
//                        ltpSuccess.lastPrice = new BigDecimal(150);
//                    }
//                    ltp.setSuccess(Collections.singletonList(ltpSuccess));
//                }
//            }
//            if (ltp != null && !CollectionUtils.isEmpty(ltp.getSuccess())) {
//                Optional<LtpSuccess> ltpOptional = ltp.getSuccess().stream().findFirst();
//                message.setPrice(String.valueOf(ltpOptional.get().getLastPrice()));
//                logger.info(" ****** [updatePriceFirstMarketOrderPlaced ] Order Execution Price := {} set for ORDER ID := {},", message.getPrice(), order.getOrderId());
//            }
//        }
    }

    private void updateSLAndOrderStatusOnKotakOrder(UserResponse user, List<OpenOrderEntity> openOrderEntityList) {
        logger.debug("Getting all orders from Kotak Securities API for User ID  := {}", user.getUserId());
        KotakGetAllOrderResponse allKotakOrders = kotakClient.getAllOrders(user.getUserId());
        if (Objects.nonNull(allKotakOrders) && !CollectionUtils.isEmpty(allKotakOrders.getSuccess())) {
            logger.debug("Get all kotak orders successful with count {} list of orders := {}", allKotakOrders.getSuccess().stream().count(), allKotakOrders.getSuccess());
            openOrderEntityList
                    .forEach(dbOrder -> {
                        KotakOrderDto order = allKotakOrders.getSuccess().stream().filter(dto -> dto.getOrderId().equals(dbOrder.getOrderId())).findAny().get();
                        logger.info("Traversing kotak orders to initiate SL order for order := {}", order.getOrderId());
                        if ((Objects.isNull(dbOrder.getPrice()) || BigDecimal.ZERO.compareTo(dbOrder.getPrice()) == 0 && !dbOrder.getIsSlOrderPlaced()) ||
                                (OrderCategoryEnum.SQAREOFF == dbOrder.getOrderCategory() && OrderStatus.OPEN == dbOrder.getOrderStatus())) {
                            OrderFeedWSMessage message = OrderFeedWSMessage.builder()
                                    .instrumentToken(order.getInstrumentToken())
                                    .instrumentType(order.getInstrumentType())
                                    .quantity(order.getOrderQuantity().toString())
                                    .price(order.getPrice().toString())
                                    .userId(user.getUserId())
                                    .messageSource(MessageSource.MANULJOB)
                                    .status(order.getStatus())
                                    .orderId(String.valueOf(order.getOrderId())).build();
                            logger.info(" ****** [orderScheduler ] Initiating process order message for Order ID {} and User Id {} ********", order.getOrderId(), user.getUserId());
                            processOrderMessage(message);
                        }
                    });
        }
    }

    /**
     * This Method will get executed after order is placed. The instrument tokens used for the orders will be used
     * to  fetch the ltp details.
     */
    @Override
    // @Async
    public void orderMonitoringScheduler() {
        logger.debug(" ****** [orderMonitoringScheduler ] Called ********");
        List<String> indexTokens = new ArrayList<>();
        Optional<AngelOneInstrumentMasterEntity> niftyBankIndexToken = instrumentService.getAllInstruments().stream()
                .filter(i -> i.getInstrumentName().equalsIgnoreCase(Constants.BANK_NIFTY_INDEX))
                .findFirst();
        if (niftyBankIndexToken.isPresent()) {
            Long niftyBankIndex = niftyBankIndexToken.get().getInstrumentToken();
            indexTokens.add(String.valueOf(niftyBankIndex));
        }
        String instrumentTokens = tradeUtils.getWebsocketInstruments().stream()
                .map(Object::toString)
                .filter(string -> !indexTokens.contains(string))
                .collect(Collectors.joining(CharConstant.DASH));
        if (StringUtils.isBlank(instrumentTokens)) {
            return;
        }
        LtpResponse ltp = kotakClient.getLtp(instrumentTokens);
        if (Objects.isNull(ltp)) {
            logger.info("KOTAK LTP Response is null hence returning from order monitor flow");
            return;
        }
        if (!CollectionUtils.isEmpty(ltp.getSuccess())) {
            ltp.getSuccess().parallelStream().filter(m -> m.getLastPrice().compareTo(BigDecimal.ZERO) > 0).forEach(ltpSuccess -> {
                logger.info("KOTAK LTP Response : InstrumentName {} and lastPrice {}", ltpSuccess.getInstrumentName(), ltpSuccess.getLastPrice());
                if (CommonUtils.getOffMarketHoursTestFlag() && CommonUtils.getTrailPriceFlag()) {
                    TradeUtils.ltpPriceIncrease = TradeUtils.ltpPriceIncrease.add(new BigDecimal("5.256"));
                    if (TradeUtils.ltpPriceIncrease.compareTo(CommonUtils.getMaximumTrailPoints()) <= 0) {
                        ltpSuccess.lastPrice = ltpSuccess.lastPrice.add(TradeUtils.ltpPriceIncrease);
                    } else {
                        TradeUtils.ltpPriceDecrease = TradeUtils.ltpPriceDecrease.add(new BigDecimal("8.5"));
                        if (TradeUtils.ltpPriceDecrease.compareTo(CommonUtils.getMaximumPriceDropPoints()) <= 0) {
                            ltpSuccess.lastPrice = ltpSuccess.lastPrice.add(TradeUtils.ltpPriceIncrease).subtract(TradeUtils.ltpPriceDecrease);
                        }
                    }
                    candleService.generateTrendData(ltpSuccess.getLastPrice());
                }
                logger.info("Before Processing LTP := {}", ltp.getSuccess());
                processLtpSuccess(ltpSuccess, String.valueOf(ltpSuccess.instrumentToken));
            });
        }
        logger.debug(" ******[ orderMonitoringScheduler ] Completed ********");
    }

    //@Async
    private void processOrderMessage(OrderFeedWSMessage orderFeedWSMessage) {
        logger.debug(" ****** [processOrderMessage ] [{}] Started ********", orderFeedWSMessage.getMessageSource());
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    strategyBuilderService.addSLAndMarginForPlacedOrder(orderFeedWSMessage);
                } catch (Exception e){
                    logger.info("******** Error {}", e.getMessage());
                    logger.error("*****[processOrderMessage] Exception for user {}", orderFeedWSMessage.getUserId());
                }
            }
        });

        logger.debug(" ****** [processOrderMessage ] Complete ********");
    }
}