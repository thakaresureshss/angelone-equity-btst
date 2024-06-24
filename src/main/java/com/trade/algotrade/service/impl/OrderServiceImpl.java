package com.trade.algotrade.service.impl;

import com.trade.algotrade.client.angelone.AngelOneClient;
import com.trade.algotrade.client.angelone.enums.Duration;
import com.trade.algotrade.client.angelone.enums.ExchangeType;
import com.trade.algotrade.client.angelone.request.orders.AngelAllOrdersResponse;
import com.trade.algotrade.client.angelone.request.orders.AngelOrderRequest;
import com.trade.algotrade.client.angelone.request.orders.AngelOrderResponse;
import com.trade.algotrade.client.angelone.request.orders.OrderDetailDto;
import com.trade.algotrade.client.angelone.response.feed.QouteResponse;
import com.trade.algotrade.client.angelone.response.user.MarginData;
import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.dto.CreateOrderDto;
import com.trade.algotrade.client.kotak.dto.KotakOrderDto;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;
import com.trade.algotrade.client.kotak.enums.OrderStatus;
import com.trade.algotrade.client.kotak.request.KotakOrderRequest;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.client.kotak.response.KotakGetOrderResponse;
import com.trade.algotrade.client.kotak.response.KotakOrderResponse;
import com.trade.algotrade.client.kotak.response.LtpResponse;
import com.trade.algotrade.constants.CharConstant;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.dto.EntryCondition;
import com.trade.algotrade.enitiy.*;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.exceptions.*;
import com.trade.algotrade.repo.AngelOneInstrumentMasterRepo;
import com.trade.algotrade.repo.ClosedOrderRepository;
import com.trade.algotrade.repo.OpenOrderRepository;
import com.trade.algotrade.repo.filter.FilterBuilder;
import com.trade.algotrade.request.OrderFilterQuery;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.request.StrategyRequest;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.StrategyResponse;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.*;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import com.trade.algotrade.utils.WebsocketUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Rahul Pansare
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    KotakClient kotakClient;

    @Autowired
    InstrumentService instrumentService;

    @Autowired
    OpenOrderRepository openOrderRepository;

    @Autowired
    ClosedOrderRepository closedOrderRepository;

    @Autowired
    UserService userService;

    @Autowired
    UserStrategyService userStrategyService;

    @Autowired
    TradeService tradeService;

    @Autowired
    StrategyService strategyService;

    @Autowired
    MarginService marginService;

    @Autowired
    WebsocketUtils websocketUtils;

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    NotificationService notificationService;

    @Autowired
    AngelOneClient angelOneClient;

    @Autowired
    AngelOneInstrumentMasterRepo angelOneInstrumentMasterRepo;

    public OrderServiceImpl() {
    }


    @Override
    public List<OrderResponse> createOrder(OrderRequest orderRequest) {
        logger.debug("Create order called for all active users With Details := {}", orderRequest);
        AngelOneInstrumentMasterEntity instrumentMasterEntity = getInstrumentAndValidateOrderBasics(orderRequest);
        OrderRequest userRequestObject = new OrderRequest();
        BeanUtils.copyProperties(orderRequest, userRequestObject);
        List<OrderResponse> responses = new ArrayList<>();
        UserResponse userResponse = userService.getUserById(orderRequest.getUserId());
        List<OrderResponse> orderResponses = validateAndCreateOrderForUser(orderRequest, instrumentMasterEntity, userResponse);
        if (!CollectionUtils.isEmpty(orderResponses))
            responses.addAll(orderResponses);
        if (CollectionUtils.isEmpty(responses)) {
            logger.info("No order created for any user, Order Category {}  ***", orderRequest.getOrderCategory());
            return new ArrayList<>();
        }
        logger.info("Total {} Orders created for Order Category {}", responses.size(), orderRequest.getOrderCategory());
        if (!orderRequest.isMockOrder()) {
            TradeUtils.activeInstruments.put(orderRequest.getInstrumentToken(), Constants.BANK_NIFTY);
        }
        if (OrderCategoryEnum.NEW == orderRequest.getOrderCategory()) {
            if (CommonUtils.getOffMarketHoursTestFlag()) {
                CommonUtils.pauseMockWebsocket = true;
                responses.forEach(response -> websocketUtils.subscribeKotakOrderWebsocketMock(response.getOrderId(), response.getUserId(), "150", "B"));
            }
        }
        if (OrderCategoryEnum.SQAREOFF == orderRequest.getOrderCategory()) {
            if (CommonUtils.getOffMarketHoursTestFlag()) {
                CommonUtils.pauseMockWebsocket = false;
            }
        }
        return responses;
    }


    private List<OrderResponse> validateAndCreateOrderForUser(OrderRequest orderRequest, AngelOneInstrumentMasterEntity kotakInstrumentMasterEntity, UserResponse userDetails) {
        logger.info("Processing Order for User > User Id := {}, Order Category :={},Order Type:={}", userDetails.getUserId(), orderRequest.getOrderCategory(), orderRequest.getOrderType());

        //TODO need to handle manual controller request not to set order quantity
        if (StringUtils.isNoneBlank(orderRequest.getStrategyName())) {
            Integer orderQuantity = getUserSpecificOrderQuantityForStrategy(kotakInstrumentMasterEntity, userDetails.getUserId(), orderRequest.getStrategyName());
            orderRequest.setQuantity(orderQuantity);
            logger.debug("{} Order Quantity Check > User Id:= {} , orderQuantity := {}", orderRequest.getStrategyName(), userDetails.getUserId(), orderQuantity);
        }

        List<OrderResponse> orderResponses = new ArrayList<>();
        List<OpenOrderEntity> orders = new ArrayList<>();
        if (isPreOrderValidationSuccess(userDetails, orderRequest)) {
            roundingQuantityToLotSize(orderRequest, kotakInstrumentMasterEntity);
            Integer orderTotalQuantity = orderRequest.getQuantity();
            //TODO Remove this twice Open trade fetch logic
            Optional<TradeEntity> openTradesForStrategyOptional = tradeService.getTradeByUserIdAndInstrumentTokenAndStrategyAndTradeStatus(userDetails.getUserId(), orderRequest.getInstrumentToken(), orderRequest.getStrategyName(), TradeStatus.OPEN);
            TradeEntity tradeEntity;
            if (openTradesForStrategyOptional.isEmpty()) {
                logger.debug("No Existing Trade found for Strategy {} and  User Id:= {} , Order Category := {}", orderRequest.getStrategyName(), userDetails.getUserId(), orderRequest.getOrderCategory());
                tradeEntity = createNewTrade(orderRequest, userDetails);
                TradeUtils.scalpOpenOrders.put(kotakInstrumentMasterEntity.getInstrumentToken(), orderRequest);
            } else {
                tradeEntity = openTradesForStrategyOptional.get();
                if (orderRequest.getOrderCategory() != OrderCategoryEnum.SQAREOFF) {
                    logger.info("Open Trade found for the new order Creation with Same USER , Strategy and Order Category :={}, User ID :={} , hence Returning from flow ", orderRequest.getOrderCategory(), userDetails.getUserId());
                    return null;
                }
            }
            //Added try-catch to continue with other users.
            try {
                processOrder(orderRequest, kotakInstrumentMasterEntity, userDetails, orderResponses, orders, orderTotalQuantity);
                updateTrade(orderRequest, orders, orderTotalQuantity, tradeEntity);
            } catch (KotakTradeApiException | KotakValidationException e) {
                logger.error("KotakTradeApiException | KotakValidationException Error {} and User Id:= {} , Order Category := {}", e.getMessage(), userDetails.getUserId(), orderRequest.getOrderCategory());
                if (openTradesForStrategyOptional.isEmpty() && CollectionUtils.isEmpty(orders)) {
                    tradeService.deleteTrade(tradeEntity);
                    TradeUtils.scalpOpenOrders.remove(kotakInstrumentMasterEntity.getInstrumentToken());
                }
            } catch (Exception e) {
                logger.error("Exception Error {} and User Id:= {} , Order Category := {}", e.getMessage(), userDetails.getUserId(), orderRequest.getOrderCategory());
                if (openTradesForStrategyOptional.isEmpty() && CollectionUtils.isEmpty(orders)) {
                    tradeService.deleteTrade(tradeEntity);
                    TradeUtils.scalpOpenOrders.remove(kotakInstrumentMasterEntity.getInstrumentToken());
                    for (OrderResponse orderResponse : orderResponses) {
                        openOrderRepository.deleteByOrderId(orderResponse.getOrderId());

                    }
                }
            }

            // This is to handle Mock Order
            if (orderRequest.isMockOrder()) {
                logger.info("MOCK ORDER : - FOUND DELETING TRADE AND ORDER");
                tradeService.deleteTrade(tradeEntity);
                for (OrderResponse orderResponse : orderResponses) {
                    openOrderRepository.deleteByOrderId(orderResponse.getOrderId());
                }
                TradeUtils.scalpOpenOrders.remove(kotakInstrumentMasterEntity.getInstrumentToken());
            }
        }
        logger.info("validateAndCreateOrderForUser Completed for User > User Id := {}, Order Category :={},Order Type:={}", userDetails.getUserId(), orderRequest.getOrderCategory(), orderRequest.getOrderType());
        return orderResponses;
    }

    private void processOrder(OrderRequest orderRequest, AngelOneInstrumentMasterEntity kotakInstrumentMasterEntity, UserResponse userDetails, List<OrderResponse> orderResponses, List<OpenOrderEntity> orders, Integer orderTotalQuantity) {
        String singleOrderQuantityLimit = commonUtils.getConfigValue(ConfigConstants.KOTAK_ORDER_QUANTITY_SIZE);
        if (singleOrderQuantityLimit == null) {
            BusinessError configMissingForKey = ErrorCodeConstants.CONFIG_MISSING_FOR_KEY;
            throw new AlgotradeException(configMissingForKey);
        }
        Integer kotakOrderQuantitySize = Integer.valueOf(singleOrderQuantityLimit);
        if (orderTotalQuantity > kotakOrderQuantitySize) {
            logger.info("Order Quantity are  := {} More Than {} Single Order Exchange Limit for User Id := {}", orderTotalQuantity, kotakOrderQuantitySize, userDetails.getUserId());
            splitOrderToMultipleOrders(kotakOrderQuantitySize, orderRequest, orderResponses, kotakInstrumentMasterEntity, userDetails, orders, orderTotalQuantity);
        } else {
            // Quantity less than 900 or equal 900 E.g. 900, 700
            logger.debug("Order Quantity are  := {} Less Than {} Single Order Exchange Limit for User Id := {}", orderTotalQuantity, kotakOrderQuantitySize, userDetails.getUserId());
            orderRequest.setQuantity(orderTotalQuantity);
            orders.add(placeAndSaveOrder(orderRequest, orderResponses, kotakInstrumentMasterEntity, userDetails));
        }
    }

    private void updateTrade(OrderRequest orderRequest, List<OpenOrderEntity> orders, Integer orderTotalQuantity, TradeEntity tradeEntity) {
        logger.info("Updating trade {} for orders {}", tradeEntity.getId(), orders.size());
        Optional<BigDecimal> maxTrendOptional = TradeUtils.trendSet.stream().sorted().max(BigDecimal::compareTo);
        tradeEntity.setMaxTrendTickDifference(maxTrendOptional.orElse(BigDecimal.ZERO));
        tradeEntity.setUniqueTrendCount(TradeUtils.trendSet.size());
        if (!CollectionUtils.isEmpty(orders) && TradeStatus.OPEN == tradeEntity.getTradeStatus() && OrderCategoryEnum.NEW == orderRequest.getOrderCategory()) {
            if (CollectionUtils.isEmpty(tradeEntity.getOrders())) {
                if (orderRequest.getTransactionType() == TransactionType.BUY) {
                    Integer existingOpenSellQuantity = tradeEntity.getSellOpenQuantityToSquareOff() == null ? 0 :
                            tradeEntity.getSellOpenQuantityToSquareOff();
                    tradeEntity.setSellOpenQuantityToSquareOff(existingOpenSellQuantity + orderTotalQuantity);
                } else {
                    Integer existingOpenBuyQuantity = tradeEntity.getBuyOpenQuantityToSquareOff() == null ? 0 :
                            tradeEntity.getBuyOpenQuantityToSquareOff();
                    tradeEntity.setBuyOpenQuantityToSquareOff(existingOpenBuyQuantity + orderTotalQuantity);
                }
                tradeEntity.setOrders(orders);
            }
            tradeService.modifyTrade(tradeEntity);
            logger.info("New trade ({}) updated for orders {}", tradeEntity.getId(), orders.size());
            // Update new Quantity
            if (orderRequest.getStrategyName().equalsIgnoreCase(Constants.STRATEGY_EXPIRY)) {
                updateStrategyLastOrderQuantity(orderRequest, orderTotalQuantity);
            }
        } else {
            logger.info("SquareOff order trade {} for orders {}", tradeEntity.getId(), orders.size());
            if (Objects.isNull(tradeEntity.getOrders())) {
                tradeEntity.setOrders(new ArrayList<>());
            }
            tradeEntity.getOrders().addAll(orders);
            tradeService.modifyTrade(tradeEntity);
        }
    }

    private void roundingQuantityToLotSize(OrderRequest orderRequest, AngelOneInstrumentMasterEntity instrumentOptionalFinal) {
        Integer lotSize = instrumentOptionalFinal.getLotSize();
        if (commonUtils.isTodayExpiryDay()) {
            orderRequest.setQuantity(lotSize);
        }
        int totalLots = orderRequest.getQuantity() / lotSize;
        orderRequest.setQuantity(lotSize * totalLots);
    }


    private void splitOrderToMultipleOrders(Integer kotakOrderQuantitySize, OrderRequest orderRequest, List<OrderResponse> mapOrderResponseList, AngelOneInstrumentMasterEntity kotakInstrumentMasterEntity, UserResponse userDetails, List<OpenOrderEntity> orders, Integer orderTotalQuantity) {
        // Quantity greater than 900 e.g. 1200, 1800
        int noOfOrders = orderTotalQuantity / kotakOrderQuantitySize; // 2
        Integer lastOrderQuantity = orderTotalQuantity % kotakOrderQuantitySize;// 0
        if (lastOrderQuantity != 0) {
            noOfOrders++;
        }
        for (int i = 0; i < noOfOrders; i++) {
            decideOrderQuantity(kotakOrderQuantitySize, orderRequest, noOfOrders, lastOrderQuantity, i);
            logger.info("USER ID := {}, CURRENT ORDER QUANTITY := {} , ORDER SPLIT NO := {}, ORIGINAL ORDER QUANTITY{}", userDetails.getUserId(), orderTotalQuantity, i + 1, orderRequest.getQuantity());
            orders.add(placeAndSaveOrder(orderRequest, mapOrderResponseList, kotakInstrumentMasterEntity, userDetails));
        }
        //After Processing request we need to update this variable to get same quantity for second or later users
        orderRequest.setQuantity(orderTotalQuantity);
    }

    private AngelOneInstrumentMasterEntity getInstrumentAndValidateOrderBasics(OrderRequest orderRequest) {
        List<AngelOneInstrumentMasterEntity> allInstruments = instrumentService.getAllInstruments();
        if (CollectionUtils.isEmpty(allInstruments)) {
            logger.info("Empty List Found > allInstruments, Hence Returning");
            throw new AlgotradeException(ErrorCodeConstants.EMPTY_INSTRUMENTS_ERROR);
        }
        logger.debug("List Found > Instruments Count := {}", allInstruments.size());
        Optional<AngelOneInstrumentMasterEntity> instrumentOptional;

        // Front end Case
        if (orderRequest.getInstrumentToken() == null || orderRequest.getInstrumentToken().compareTo(0L) == 0) {
            logger.info("Endpoint/API Order Processing");
            instrumentOptional = allInstruments.stream().filter(i -> i.getInstrumentName().equalsIgnoreCase(orderRequest.getInstrumentName()) && i.getStrike().compareTo(orderRequest.getStrikePrice()) == 0 && i.getOptionType().equalsIgnoreCase(orderRequest.getOptionType().toString())).findFirst();
        } else {
            // Backend Case
            logger.info("Scheduler/Service Order Processing");
            instrumentOptional = allInstruments.stream().filter(i -> i.getInstrumentToken() == orderRequest.getInstrumentToken()).findFirst();
        }

        if (instrumentOptional.isEmpty()) {
            logger.info("Empty Instrument Found > instrumentOptional");
            ValidationError violations = new ValidationError();
            violations.setField("instrumentName");
            violations.setMessage("Invalid or Not found in instrument list");
            violations.setRejectedValue(orderRequest.getInstrumentName());
            throw new AlgoValidationException(new BusinessError(HttpStatus.BAD_REQUEST, List.of(violations)));
        }
        return instrumentOptional.get();
    }

    public Integer getUserSpecificOrderQuantityForStrategy(AngelOneInstrumentMasterEntity kotakInstrumentMasterEntity, String userId, String strategyName) {
        Integer orderQuantity;
        UserStrategyEntity userStrategyEntity = userStrategyService.findStrategyByUserIdAndStrategy(userId, strategyName);
        switch (strategyName) {
            case Constants.BIG_CANDLE_STRATEGY: {
                if (userStrategyEntity != null) {
                    logger.debug("{} strategy for user : {}, quantity : {}", strategyName, userId, userStrategyEntity.getQuantity());
                    BigDecimal overallLoss = Objects.nonNull(userStrategyEntity.getOverallLoss()) ? userStrategyEntity.getOverallLoss() : BigDecimal.ZERO;
                    if (overallLoss.compareTo(BigDecimal.ZERO) > 0) {
                        TradeUtils.lossRecoveryQuantity = recoveryStrategyQuantityFinder(kotakInstrumentMasterEntity, userStrategyEntity, overallLoss);
                        logger.info("Recovery strategy :: quantity {}", TradeUtils.lossRecoveryQuantity);
                    } else {
                        TradeUtils.lossRecoveryQuantity = userStrategyEntity.getQuantity();
                    }
                    if (commonUtils.isTodayExpiryDay()) {
                        return kotakInstrumentMasterEntity.getLotSize();
                    }
                    return userStrategyEntity.getQuantity();
                } else {
                    return kotakInstrumentMasterEntity.getLotSize();
                }
            }

            case Constants.STRATEGY_MANUAL: {
                if (commonUtils.isTodayExpiryDay()) {
                    return kotakInstrumentMasterEntity.getLotSize();
                }
                if (userStrategyEntity != null) {
                    logger.debug("{} strategy for user : {}, quantity : {}", strategyName, userId, userStrategyEntity.getQuantity());
                    BigDecimal tradeCapital = userStrategyEntity.getOverallProfit().add(userStrategyEntity.getInitialCapitalDeployed());
                    QouteResponse ltp = angelOneClient.getQuote(ExchangeType.NFO, List.of(String.valueOf(kotakInstrumentMasterEntity.getInstrumentToken())));
                    BigDecimal lastPrice = null;
                    if (ltp != null && ltp.getData() != null && !CollectionUtils.isEmpty(ltp.getData().getFetched())) {
                        lastPrice = ltp.getData().getFetched().get(0).ltp;
                    }
                    if (lastPrice == null) {
                        return kotakInstrumentMasterEntity.getLotSize();
                    }
                    orderQuantity = tradeCapital.divide(lastPrice, 2, RoundingMode.DOWN).setScale(0, RoundingMode.UP).intValue();
                    int lotSize = kotakInstrumentMasterEntity.getLotSize();
                    int quantityMod = orderQuantity % lotSize;
                    int initialLots = orderQuantity / lotSize;
                    return (initialLots * lotSize) + (lotSize / 2 > quantityMod ? 0 : lotSize);
                } else {
                    return kotakInstrumentMasterEntity.getLotSize();
                }
            }
            case Constants.STRATEGY_EXPIRY: {
                StrategyResponse strategy = strategyService.getStrategy(strategyName);
                EntryCondition entryCondition = strategy.getEntryCondition();
                if (entryCondition == null) {
                    throw new AlgotradeException(ErrorCodeConstants.ENTRY_CONDITION_NOT_FOUND);
                }
                Map<String, String> entryConditions = entryCondition.getConditions();
                if (CollectionUtils.isEmpty(entryConditions)) {
                    throw new AlgotradeException(ErrorCodeConstants.ENTRY_CONDITION_NOT_FOUND);
                }
                if (userStrategyEntity != null) {
                    orderQuantity = userStrategyEntity.getQuantity();
                } else {
                    orderQuantity = Integer.valueOf(entryCondition.getConditions().get(Constants.DEFAULT_QUANTITY));
                }
                String lastOrderedQuantity = entryConditions.get(Constants.LAST_ORDER_QUANTITY);
                BigDecimal finalOrderQuantity;
                if (lastOrderedQuantity == null || Integer.parseInt(lastOrderedQuantity) == 0) {
                    finalOrderQuantity = new BigDecimal(orderQuantity);
                } else {
                    finalOrderQuantity = Constants.ONE_POINT_TWENTY_FIVE.multiply(BigDecimal.valueOf(Integer.parseInt(lastOrderedQuantity)));
                }
                return finalOrderQuantity.intValue();
            }
            default:
                return Constants.GENERIC_DEFAULT_QUANTITY;
        }

    }

    private int recoveryStrategyQuantityFinder(AngelOneInstrumentMasterEntity kotakInstrumentMasterEntity, UserStrategyEntity userStrategyEntity, BigDecimal overallLoss) {
        int bufferQuantityPercentage = userStrategyEntity.getBufferQuantityPercentage() != 0 ? userStrategyEntity.getBufferQuantityPercentage() : 10;
        BigDecimal divide = overallLoss.divide(new BigDecimal(commonUtils.getConfigValue(ConfigConstants.SCALPING_TARGET_POINTS)), 2, RoundingMode.DOWN).setScale(0, RoundingMode.UP);
        int orderQuantity = divide.add(divide.multiply(new BigDecimal(bufferQuantityPercentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN))).intValue();
        int lotSize = kotakInstrumentMasterEntity.getLotSize();
        int quantityMod = orderQuantity % lotSize;
        int initialLots = orderQuantity / lotSize;
        return (initialLots * lotSize) + (lotSize / 2 > quantityMod ? 0 : lotSize);
    }

    private void updateStrategyLastOrderQuantity(OrderRequest orderRequest, Integer totalQuantity) {

        StrategyResponse strategy = strategyService.getStrategy(orderRequest.getStrategyName());

        if (TransactionType.BUY == orderRequest.getTransactionType() && orderRequest.getOrderCategory() == OrderCategoryEnum.NEW) {

            logger.info("Updating last traded Quantity Buy order for Strategy := {} , Last ordered quantity :={}", orderRequest.getStrategyName(), totalQuantity);
            strategy.getEntryCondition().getConditions().put(Constants.LAST_ORDER_QUANTITY, totalQuantity.toString());
            AngelOneInstrumentMasterEntity byInstrumentName = instrumentService.findByInstrumentName(Constants.BANK_NIFTY_INDEX);
            if (byInstrumentName != null) {
                LtpResponse ltp = kotakClient.getLtp(String.valueOf(byInstrumentName.getInstrumentToken()));
                if (ltp != null && ltp.getSuccess() != null && !CollectionUtils.isEmpty(ltp.getSuccess())) {
                    Optional<LtpSuccess> instrumentLtpOptional = ltp.getSuccess().stream().findFirst();
                    BigDecimal lastPrice = instrumentLtpOptional.get().getLastPrice();
                    strategy.getEntryCondition().getConditions().put(Constants.LAST_BANK_NIFTY_INDEX_VALUE, String.valueOf(lastPrice));
                }
            }

            StrategyRequest strategyRequest = new StrategyRequest();
            strategyRequest.setEntryCondition(strategy.getEntryCondition());
            strategyRequest.setExitCondition(strategy.getExitCondition());
            strategyRequest.setStrategyName(strategy.getStrategyName());
            strategyService.modifyStrategy(orderRequest.getStrategyName(), strategyRequest);
            logger.info("Updated last traded Quantity for Strategy {} , Last ordered quantity {}", orderRequest.getStrategyName(),
                    totalQuantity);
        } else if (TransactionType.SELL == orderRequest.getTransactionType() && orderRequest.getOrderCategory() == OrderCategoryEnum.SQAREOFF) {

            logger.info("Updating last traded Quantity Sell Order for Strategy := {} , Last ordered quantity :={}", orderRequest.getStrategyName(), totalQuantity);
            boolean isTradeSuccess = false;
            if (isTradeSuccess) {
                strategy.getEntryCondition().getConditions().put(Constants.LAST_ORDER_QUANTITY,
                        BigDecimal.ZERO.toString());
                StrategyRequest strategyRequest = new StrategyRequest();
                strategyRequest.setEntryCondition(strategy.getEntryCondition());
                strategyRequest.setExitCondition(strategy.getExitCondition());
                strategyRequest.setStrategyName(strategy.getStrategyName());
                strategyService.modifyStrategy(orderRequest.getStrategyName(), strategyRequest);
                // Find PNL if Trade is SL then don't update quantity , If trade is not SL order then Mark total Quantity as 0
                logger.info("Updated last traded Quantity for Strategy {} , Last ordered quantity {}", orderRequest.getStrategyName(),
                        totalQuantity);
            }

        }


    }

    private void decideOrderQuantity(Integer kotakOrderQuantitySize, OrderRequest orderRequest, Integer noOfOrders, Integer lastOrderQuantity, int i) {
        if (i < noOfOrders - 1) { // 1
            orderRequest.setQuantity(kotakOrderQuantitySize);
        } else {
            if (lastOrderQuantity == 0) {
                orderRequest.setQuantity(kotakOrderQuantitySize);
            } else {
                orderRequest.setQuantity(lastOrderQuantity);
            }
        }
    }

    // @CacheEvict(value = "openOrders", allEntries = true)
    public OpenOrderEntity placeAndSaveOrder(OrderRequest orderRequest, List<OrderResponse> mapOrderResponseList, AngelOneInstrumentMasterEntity instrument, UserResponse userDetails) {
        logger.info("Inside placeAndSaveOrder User ID {} Order Request {}, ", userDetails.getUserId(), orderRequest);
        AngelOrderResponse createdOrder = placeOrder(orderRequest, userDetails);
        OrderDetailDto orderDetailDto = createdOrder.getData();
        Long orderID = orderDetailDto.getOrderId();
        OptionType fromValue = OptionType.fromValue(instrument.getOptionType());
        //this is SL order case
        OpenOrderEntity orderEntity = OpenOrderEntity.builder().build();
        mapOrderToEntity(orderRequest, instrument, userDetails, orderID, fromValue, orderEntity);

        OrderResponse mapOrderResponse = mapOrderResponse(createdOrder);
        if (orderRequest.getOrderType() == OrderType.SL) {
//            List<SlDetails> slDetailsHistory = orderEntity.getSlDetailsHistory();
//            if (Objects.isNull(slDetailsHistory)) {
//                slDetailsHistory = new ArrayList<>();
//            }
//            slDetailsHistory.add(orderEntity.getSlDetails());
//            orderEntity.setSlDetailsHistory(slDetailsHistory);
            orderEntity.setIsSL(Boolean.TRUE);
        }
        orderEntity.setUpdatedAt(DateUtils.getCurrentDateTimeIst());
        OpenOrderEntity order;
        order = openOrderRepository.save(orderEntity);
        mapOrderResponse.setUserId(userDetails.getUserId());
        mapOrderResponseList.add(mapOrderResponse);
        logger.info("Order placed for User ID {}, Order {},  At Time {}, for {} order and saved to DB ", order.getUserId(), order.getOrderId(), DateUtils.getCurrentDateTimeIst(), order.getOrderCategory());
        return order;
    }

    public OpenOrderEntity placeDirectSLOrder(OrderRequest orderRequest, UserResponse userDetails) {
        logger.info("Inside placeDirectSLOrder User ID {} Order Request {}, ", userDetails.getUserId(), orderRequest);
        AngelOrderResponse createdOrder = placeOrder(orderRequest, userDetails);
        OrderDetailDto orderDetailDto = createdOrder.getData();
        Long orderID = orderDetailDto.getOrderId();
        //Verified for big candle its new object no need to fetch from DB.
        // Verify for Equity and remove if not required.
        OpenOrderEntity orderEntity = OpenOrderEntity.builder().build();
        mapOrderToEntity(orderRequest, userDetails, orderID, orderRequest.getOptionType(), orderEntity);
        OrderResponse mapOrderResponse = mapOrderResponse(createdOrder);

//        if (orderEntity.getPrice() == null || orderEntity.getPrice().compareTo(BigDecimal.ZERO) == 0) {
//            orderEntity.setPrice(mapOrderResponse.getPrice());
//        }
        if (orderRequest.getOrderType() == OrderType.SL) {
//            List<SlDetails> slDetailsHistory = orderEntity.getSlDetailsHistory();
//            if (Objects.isNull(slDetailsHistory)) {
//                slDetailsHistory = new ArrayList<>();
//            }
//            slDetailsHistory.add(orderEntity.getSlDetails());
//            orderEntity.setSlDetailsHistory(slDetailsHistory);
            orderEntity.setIsSL(Boolean.TRUE);
        }
        orderEntity.setUpdatedAt(DateUtils.getCurrentDateTimeIst());
        OpenOrderEntity order = openOrderRepository.save(orderEntity);
        logger.info("Direct SL Order placed for User ID {}, Order {},  At Time {}, for {} order and saved to DB ", order.getUserId(), order.getOrderId(), DateUtils.getCurrentDateTimeIst(), order.getOrderCategory());
        mapOrderResponse.setUserId(userDetails.getUserId());
        Optional<TradeEntity> openTradesForStrategyOptional = tradeService.getTradeByUserIdAndInstrumentTokenAndStrategyAndTradeStatus(userDetails.getUserId(), orderRequest.getInstrumentToken(), orderRequest.getStrategyName(), TradeStatus.OPEN);
        if (openTradesForStrategyOptional.isPresent()) {
            TradeEntity newTradeCreated = openTradesForStrategyOptional.get();
            updateTrade(orderRequest, List.of(order), orderRequest.getQuantity(), newTradeCreated);
        }
        if (CommonUtils.getOffMarketHoursTestFlag()) {
            CommonUtils.pauseMockWebsocket = false;
        }
        return order;
    }

    private TradeEntity createNewTrade(OrderRequest orderRequest, UserResponse userDetails) {
        TradeEntity tradeOrderEntity = TradeEntity.builder().build();
        tradeOrderEntity.setTradeStatus(TradeStatus.OPEN);
        tradeOrderEntity.setInstrumentToken(orderRequest.getInstrumentToken());
        tradeOrderEntity.setUserId(userDetails.getUserId());
        tradeOrderEntity.setCreatedTime(LocalDateTime.now());
        tradeOrderEntity.setStrategy(orderRequest.getStrategyName());
        tradeOrderEntity.setTradeId(UUID.randomUUID().toString());
        return tradeService.createTrade(tradeOrderEntity);
    }

    private AngelOrderResponse placeOrder(OrderRequest orderRequest,
                                          UserResponse userDetails) {
        orderRequest.setInstrumentToken(orderRequest.getInstrumentToken());
        SlDetails slDetails = orderRequest.getSlDetails();

//        KotakOrderRequest kotakOrderRequest = mapKotakOrderRequest(orderRequest);
        AngelOrderRequest angelOrderRequest = mapAngelOrderRequest(orderRequest);

        if (slDetails != null && orderRequest.getOrderType() == OrderType.LIMIT) {
            BigDecimal triggerPrice = slDetails.getTriggerPrice();
            angelOrderRequest.setTriggerPrice(triggerPrice);
        }

        //KotakOrderResponse createdOrder = kotakClient.placeOrder(kotakOrderRequest, userDetails);
        AngelOrderResponse angelOrderResponse = angelOneClient.placeOrder(angelOrderRequest, userDetails);
        if (angelOrderResponse == null || !StringUtils.isEmpty(angelOrderResponse.getErrorCode())) {
            logger.error("Angel Place order Exception Angel order Request {}, User ID {}", angelOrderRequest, userDetails.getUserId());
            BusinessError kotakOrderError = ErrorCodeConstants.KOTAK_CREATE_ORDER_ERROR;
            kotakOrderError.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            throw new KotakTradeApiException(kotakOrderError);
        }
        String message = MessageFormat.format("{0} order placed with order id {1}, quantity {2}, option type {3}", orderRequest.getTransactionType(), Long.toString(angelOrderResponse.getData().getOrderId()), orderRequest.getQuantity(), orderRequest.getOptionType());
        notificationService.sendTelegramNotification(userDetails.getTelegramChatId(), message);
        return angelOrderResponse;
    }

    private void mapOrderToEntity(OrderRequest orderRequest, AngelOneInstrumentMasterEntity instrumentOptionalFinal, UserResponse userDetails, Long orderID, OptionType fromValue, OpenOrderEntity orderEntity) {
        mapOrderToEntity(orderRequest, userDetails, orderID, fromValue, orderEntity);
        orderEntity.setStrikePrice(instrumentOptionalFinal.getStrike() != null ? instrumentOptionalFinal.getStrike() : Integer.valueOf(0));
    }

    private static void mapOrderToEntity(OrderRequest orderRequest, UserResponse userDetails, Long orderID, OptionType fromValue, OpenOrderEntity orderEntity) {
        orderEntity.setOrderId(orderID);
        orderEntity.setUserId(userDetails.getUserId());
        orderEntity.setInstrumentToken(orderRequest.getInstrumentToken());
        orderEntity.setQuantity(orderRequest.getQuantity());
        orderEntity.setTransactionType(orderRequest.getTransactionType());
        orderEntity.setVariety(orderRequest.getVariety());
        orderEntity.setOptionType(fromValue);
        orderEntity.setPrice(orderRequest.getPrice());
        orderEntity.setOrderStatus(com.trade.algotrade.enums.OrderStatus.OPEN);
        orderEntity.setStrategyName(orderRequest.getStrategyName());
        orderEntity.setSlDetails(orderRequest.getSlDetails());
        orderEntity.setTargetDetails(orderRequest.getTargetDetails());
        orderEntity.setOrderType(orderRequest.getOrderType());
        orderEntity.setSegment(orderRequest.getSegment());
        orderEntity.setOrderCategory(orderRequest.getOrderCategory());
        orderEntity.setIsSlOrderPlaced(Boolean.FALSE);
        orderEntity.setOriginalOrderId(orderRequest.getOriginalOrderId());
        orderEntity.setCreatedAt(DateUtils.getCurrentDateTimeIst());
    }

    private KotakOrderRequest mapKotakOrderRequest(OrderRequest orderRequest) {
        KotakOrderRequest newOrder = new KotakOrderRequest();
        newOrder.setOrderId(orderRequest.getOrderId());
        newOrder.setInstrumentToken(orderRequest.getInstrumentToken());
        if (orderRequest.getOrderType() == OrderType.MARKET) {
            newOrder.setPrice(BigDecimal.ZERO);
            newOrder.setTriggerPrice(BigDecimal.ZERO);
        } else {
            AngelOneInstrumentMasterEntity kotakInstrumentMasterEntity = instrumentService.getInstrumentByToken(orderRequest.getInstrumentToken());
            newOrder.setPrice(CommonUtils.roundPriceValueInMultipleOfTickSize(orderRequest.getSlDetails().getSlPrice(), kotakInstrumentMasterEntity.getTickSize(), RoundingMode.UP));
            newOrder.setTriggerPrice(CommonUtils.roundPriceValueInMultipleOfTickSize(orderRequest.getPrice(), kotakInstrumentMasterEntity.getTickSize(), RoundingMode.UP));
        }
        newOrder.setQuantity(orderRequest.getQuantity());
        newOrder.setProduct(com.trade.algotrade.client.angelone.enums.ProductType.fromValue(orderRequest.getProduct().toString()));
//        newOrder.setValidity(ValidityEnum.fromValue(orderRequest.getValidity().toString()));
        newOrder.setVariety(VarietyEnum.fromValue(orderRequest.getVariety().toString()));
        newOrder.setTag(orderRequest.getStrategyName());
        newOrder.setTransactionType(TransactionType.fromValue(orderRequest.getTransactionType().toString()));
        newOrder.setDisclosedQuantity(0L);
        logger.debug("Modify Kotak order request object : {}", newOrder);
        return newOrder;
    }

    private AngelOrderRequest mapAngelOrderRequest(OrderRequest orderRequest) {
        AngelOrderRequest newOrder = new AngelOrderRequest();
        newOrder.setOrderId(orderRequest.getOrderId());
        newOrder.setMockOrder(orderRequest.isMockOrder());
        AngelOneInstrumentMasterEntity angelOneInstrumentMasterEntity = angelOneInstrumentMasterRepo.findByInstrumentToken(orderRequest.getInstrumentToken());
        if (Objects.isNull(angelOneInstrumentMasterEntity)) {
            logger.info("INSTRUMENT NOT FOUND for TOKEN {}", orderRequest.getInstrumentToken());
            throw new AlgotradeException(ErrorCodeConstants.EMPTY_INSTRUMENTS_ERROR);
        }
        newOrder.setTradingSymbol(angelOneInstrumentMasterEntity.getTradingSymbol());
        newOrder.setVariety(orderRequest.getVariety());
        newOrder.setSymbolToken(String.valueOf(angelOneInstrumentMasterEntity.getInstrumentToken()));
        if (orderRequest.getOrderType() == OrderType.MARKET) {
            newOrder.setPrice(BigDecimal.ZERO);
            newOrder.setTriggerPrice(BigDecimal.ZERO);
        } else {
            newOrder.setPrice(CommonUtils.roundPriceValueInMultipleOfTickSize(orderRequest.getSlDetails().getSlPrice(), angelOneInstrumentMasterEntity.getTickSize(), RoundingMode.UP));
            //TODO orderRequest.getPrice() can be changed to orderRequest.getSlDetails().getTriggerPrice()
            newOrder.setTriggerPrice(CommonUtils.roundPriceValueInMultipleOfTickSize(orderRequest.getPrice(), angelOneInstrumentMasterEntity.getTickSize(), RoundingMode.UP));
        }
        newOrder.setExchange(ExchangeType.NFO);
        newOrder.setOrdertype(orderRequest.getOrderType());
        newOrder.setQuantity(orderRequest.getQuantity());
        newOrder.setDuration(Duration.DAY.toString());
        newOrder.setProduct(com.trade.algotrade.client.angelone.enums.ProductType.fromValue(orderRequest.getProduct().toString()));
        newOrder.setTransactionType(TransactionType.fromValue(orderRequest.getTransactionType().toString()));
        newOrder.setOrderTag(orderRequest.getTag());
        logger.debug("Map Angel order request object : {}", newOrder);
        return newOrder;
    }


    private OrderResponse mapOrderResponse(AngelOrderResponse order) {
        OrderResponse orderResponse = new OrderResponse();
        if (order != null && Objects.nonNull(order.getData())) {
            OrderDetailDto createOrderDto = order.getData();
            orderResponse.setOrderId(createOrderDto.getOrderId());
        }
        return orderResponse;
    }

    @Override
    public List<OrderResponse> modifyOrder(Long orderId, OrderRequest orderRequest, String squareOffReason, boolean directOrder) {

        List<OrderResponse> mapOrderResponseList = new ArrayList<>();
        Optional<OpenOrderEntity> orderOptional = openOrderRepository
                .findByOrderIdAndOrderStatusAndCreatedAtGreaterThanEqual(orderId,
                        com.trade.algotrade.enums.OrderStatus.OPEN, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));

        if (orderOptional.isEmpty()) {
            logger.info("Order provided to modify is not in OPEN status Hence Returning from modification flow, Order ID  {}, {}", orderId, orderOptional);
            return mapOrderResponseList;
        }
        logger.info("modifyOrder > Modifying Order {} With Data {}", orderId, orderRequest);
        OpenOrderEntity openOrderEntity = orderOptional.get();
        UserResponse userDetails = userService.getUserById(openOrderEntity.getUserId());
        int oldModificationCount = openOrderEntity.getModificationCount();
        if (openOrderEntity.getOrderCategory() == OrderCategoryEnum.SQAREOFF && !StringUtils.isEmpty(openOrderEntity.getSquareOffReason())) {
            logger.info("Square off order is already completed with reason := {} , Hence not updating order anymore", openOrderEntity.getSquareOffReason());
            //Setting empty object to execute, update SL order and trade flow.
            mapOrderResponseList.add(new OrderResponse());
            return mapOrderResponseList;
        }

        if (directOrder) {
            modifySLOrderToMarketToKotak(orderRequest, squareOffReason, userDetails, mapOrderResponseList, openOrderEntity, oldModificationCount);
        } else {
            String brokerModificationCount = commonUtils.getConfigValue(ConfigConstants.KOTAK_MAX_ORDER_MODIFICATION_COUNT);
            if (brokerModificationCount == null) {
                throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
            }
            if (oldModificationCount >= Integer.parseInt(brokerModificationCount)) {
                cancelOrder(orderId, userDetails.getUserId());
                createNewOrder(orderRequest, openOrderEntity, userDetails);
            } else {
                modifySLOrderToMarketToKotak(orderRequest, squareOffReason, userDetails, mapOrderResponseList, openOrderEntity, oldModificationCount);
            }
        }
        String message = MessageFormat.format("Modify call for order id {0}, reason {1}", Long.toString(orderId), squareOffReason);
        notificationService.sendTelegramNotification(userDetails.getTelegramChatId(), message);
        return mapOrderResponseList;
    }

    private void modifySLOrderToMarketToKotak(OrderRequest orderRequest, String squareOffReason, UserResponse userDetails, List<OrderResponse> mapOrderResponseList, OpenOrderEntity openOrderEntity, int oldModificationCount) {
//        KotakOrderResponse modifyOrder = kotakClient.modifyOrder(mapKotakOrderRequest(orderRequest),
//                userDetails.getUserId());
        AngelOrderResponse modifyOrder = angelOneClient.modifyOrder(mapAngelOrderRequest(orderRequest), userDetails.getUserId());
        if (Objects.nonNull(modifyOrder) && Objects.nonNull(modifyOrder.getData())) {
            // As per discussion this code is not useful now
//                if (openOrderEntity.getOrderType() == OrderType.LIMIT)
//                    updateMargin(orderRequest, userDetails, openOrderEntity);
            OrderResponse order = new OrderResponse();
            order.setOrderId(modifyOrder.getData().getOrderId());
            order.setQuantity(orderRequest.getQuantity());
            order.setInstrumentToken(orderRequest.getInstrumentToken());
            order.setUserId(userDetails.getUserId());
            mapOrderResponseList.add(order);
            updateSLDetailsOfExistingOrder(orderRequest, openOrderEntity);
            openOrderEntity.setOrderType(orderRequest.getOrderType());
            openOrderEntity.setPrice(orderRequest.getPrice());
            openOrderEntity.setUpdatedAt(DateUtils.getCurrentDateTimeIst());
            openOrderEntity.setModificationCount(oldModificationCount + 1);
            openOrderEntity.setSquareOffReason(squareOffReason);
            openOrderRepository.save(openOrderEntity);
        }

//        if (Constants.MODIFY_ORDER_ERROR_IF_ALREADY_FIL.equals(modifyOrder.getFault().getMessage().trim())) {
//
//        }
    }


    private static void updateSLDetailsOfExistingOrder(OrderRequest orderRequest, OpenOrderEntity openOrderEntity) {
        SlDetails slDetails = openOrderEntity.getSlDetails();
        SlDetails orderRequestSLDetails = orderRequest.getSlDetails();
        if (orderRequestSLDetails != null) {
            slDetails.setTriggerPrice(orderRequestSLDetails.getTriggerPrice());
            if (Objects.nonNull(orderRequest.getPrice()))
                slDetails.setSlPrice(orderRequestSLDetails.getSlPrice());
            else slDetails.setSlPrice(BigDecimal.ZERO);
            if (openOrderEntity.getOrderCategory() == OrderCategoryEnum.SQAREOFF && Objects.nonNull(orderRequest.getPrice())) {
                slDetails.setSlNextTargetPrice(orderRequestSLDetails.getSlNextTargetPrice());
            }
            openOrderEntity.setSlDetails(slDetails);
            List<SlDetails> slDetailsHistory = openOrderEntity.getSlDetailsHistory();
            if (!CollectionUtils.isEmpty(slDetailsHistory)) {
                slDetailsHistory.add(slDetails);
            }
        }
    }

    @Override
    public void modifyOrderFromController() {
        KotakOrderRequest newOrder = new KotakOrderRequest();
        newOrder.setOrderId(1230922073157l);
        newOrder.setPrice(new BigDecimal(190));
        newOrder.setTriggerPrice(new BigDecimal(160));
        newOrder.setQuantity(15);
//        newOrder.setProduct(com.trade.algotrade.client.angelone.enums.ProductType.NORMAL);
        newOrder.setValidity(ValidityEnum.GFD);
        newOrder.setVariety(VarietyEnum.REGULAR);
        newOrder.setDisclosedQuantity(0L);
        KotakOrderResponse modifyOrder = kotakClient.modifyOrder(newOrder,
                "ST27101989");
    }

    private void createNewOrder(OrderRequest orderRequest, OpenOrderEntity openOrderEntity, UserResponse userDetails) {
        KotakOrderRequest kotakOrderRequest = mapKotakOrderRequest(orderRequest);
        KotakOrderResponse createdOrder = kotakClient.placeOrder(kotakOrderRequest, userDetails);
        if (Objects.nonNull(createdOrder)) {
            String oldOrderId = openOrderEntity.getId();
            CreateOrderDto createOrderDto = createdOrder.getSuccess().get(Constants.NSE);
            openOrderEntity.setOrderId(createOrderDto.getOrderId());
            openOrderEntity.setModificationCount(0);
            openOrderEntity.setId(null);
            openOrderEntity.setUpdatedAt(LocalDateTime.now());
            openOrderEntity.setCreatedAt(LocalDateTime.now());
            openOrderEntity.setPrice(createOrderDto.getPrice());
            OpenOrderEntity savedOrderEntity = openOrderRepository.save(openOrderEntity);
            Optional<TradeEntity> optionalTrade = tradeService.getTradeForOrder(oldOrderId);
            if (optionalTrade.isPresent()) {
                TradeEntity tradeEntity = optionalTrade.get();
                tradeEntity.getOrders().add(savedOrderEntity);
                tradeService.modifyTrade(tradeEntity);
            }
            logger.info("[createNewOrder] Created new order {} for cancelled order with Order ID :={} and USER ID := {}", openOrderEntity.getOrderId(), createOrderDto.getOrderId(), openOrderEntity.getUserId());
        }
    }

    // Commenting this code to not track this in JACOCO report for now.

    /**
     * private void updateMargin(OrderRequest orderRequest, UserResponse userDetails, OpenOrderEntity openOrderEntity) {
     * if (TransactionType.BUY == orderRequest.getTransactionType()
     * && openOrderEntity.getTransactionType() == TransactionType.BUY) {
     * <p>
     * BigDecimal newMargin = marginService.getMargin(userDetails.getUserId())
     * .add(orderRequest.getPrice().multiply(new BigDecimal(orderRequest.getQuantity())));
     * <p>
     * BigDecimal oldMargin = marginService.getMargin(userDetails.getUserId())
     * .add(openOrderEntity.getPrice().multiply(new BigDecimal(openOrderEntity.getQuantity())));
     * <p>
     * BigDecimal currentMargin = marginService.getMargin(userDetails.getUserId());
     * BigDecimal updatedMargin = currentMargin.add(oldMargin).subtract(newMargin);
     * marginService.updateMargin(userDetails.getUserId(), updatedMargin);
     * <p>
     * } else if (TransactionType.SELL == orderRequest.getTransactionType()
     * && openOrderEntity.getTransactionType() == TransactionType.SELL) {
     * <p>
     * BigDecimal newMargin = marginService.getMargin(userDetails.getUserId())
     * .add(orderRequest.getPrice().multiply(new BigDecimal(orderRequest.getQuantity())));
     * <p>
     * BigDecimal oldMargin = marginService.getMargin(userDetails.getUserId())
     * .add(openOrderEntity.getPrice().multiply(new BigDecimal(openOrderEntity.getQuantity())));
     * BigDecimal currentMargin = marginService.getMargin(userDetails.getUserId());
     * BigDecimal updatedMargin = currentMargin.add(oldMargin).subtract(newMargin);
     * marginService.updateMargin(userDetails.getUserId(), updatedMargin);
     * <p>
     * } else if (TransactionType.BUY == orderRequest.getTransactionType()
     * && openOrderEntity.getTransactionType() == TransactionType.SELL) {
     * <p>
     * BigDecimal newMargin = marginService.getMargin(userDetails.getUserId())
     * .add(orderRequest.getPrice().multiply(new BigDecimal(orderRequest.getQuantity())));
     * BigDecimal currentMargin = marginService.getMargin(userDetails.getUserId());
     * BigDecimal updatedMargin = currentMargin.subtract(newMargin);
     * marginService.updateMargin(userDetails.getUserId(), updatedMargin);
     * } else if (TransactionType.SELL == orderRequest.getTransactionType()
     * && openOrderEntity.getTransactionType() == TransactionType.BUY) {
     * BigDecimal newMargin = marginService.getMargin(userDetails.getUserId())
     * .add(orderRequest.getPrice().multiply(new BigDecimal(orderRequest.getQuantity())));
     * BigDecimal oldMargin = marginService.getMargin(userDetails.getUserId())
     * .add(openOrderEntity.getPrice().multiply(new BigDecimal(openOrderEntity.getQuantity())));
     * BigDecimal currentMargin = marginService.getMargin(userDetails.getUserId());
     * BigDecimal updatedMargin = currentMargin.add(oldMargin).add(newMargin);
     * marginService.updateMargin(userDetails.getUserId(), updatedMargin);
     * }
     * }
     **/

    @Override
    public OrderResponse getOrder(Long orderId, String userId) {
        // Fist check in local DB
        KotakGetOrderResponse orderDetailsById = kotakClient.getOrderDetailsById(orderId, userId, true);
        if (orderDetailsById != null && !CollectionUtils.isEmpty(orderDetailsById.getSuccess())) {
            return orderDetailsById.getSuccess().stream().map(this::mapGetOrderResponse).collect(Collectors.toList())
                    .get(0);
        }
        return null;
    }

    @Override
    //  @CacheEvict(value = "openOrders", allEntries = true)
    public void cancelOrder(Long orderId, String userId) {
        logger.info("[cancelOrder] Cancelling order with Order ID :={} and USER ID := {}", orderId, userId);
        Optional<OpenOrderEntity> optionalOpenOrderEntity = openOrderRepository.findByOrderId(orderId);
        if (optionalOpenOrderEntity.isEmpty()) {
            throw new AlgoValidationException(ErrorCodeConstants.ORDER_NOT_FOUND_ERROR);
        }
        OpenOrderEntity openOrderEntity = optionalOpenOrderEntity.get();
        openOrderEntity.setOrderStatus(com.trade.algotrade.enums.OrderStatus.CANCELLED);
        openOrderEntity.setUpdatedAt(DateUtils.getCurrentDateTimeIst());
        openOrderRepository.save(openOrderEntity);
//        kotakClient.cancelOrder(orderId, userId);
        angelOneClient.cancelOrder(orderId, userId);
    }

    @Override
    public List<OrderResponse> getAllOrders(String userId) {
        //KotakGetAllOrderResponse allOrders = kotakClient.getAllOrders(userId);
        AngelAllOrdersResponse allOrders = angelOneClient.getAllOrders(userId);
        if (allOrders != null && !CollectionUtils.isEmpty(allOrders.getData())) {
            return allOrders.getData().stream().map(this::mapGetAngelOrderResponse).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private OrderResponse mapGetAngelOrderResponse(OrderDetailDto o) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrderId(o.getOrderId());
        if (o.getTransactionType() != null && !StringUtils.isEmpty(o.getTransactionType())) {
            orderResponse.setTransactionType(TransactionType.fromValue(o.getTransactionType()));
        }
        orderResponse.setExpiryDate(o.getExpiryDate());
        orderResponse.setOrderTimestamp(o.getExchOrderUpdateTime());
        if (o.getVariety() != null && !StringUtils.isEmpty(o.getVariety())) {
            orderResponse.setVariety(com.trade.algotrade.client.angelone.enums.VarietyEnum.fromValue(o.getVariety()));
        }
        orderResponse.setPrice(o.getAveragePrice());
//        if (o.getValidity() != null && !o.getValidity().equalsIgnoreCase(CharConstant.DASH)) {
//            String input = o.getValidity();
//            ValidityEnum output = null;
//            if (input.equals("Good For Day")) {
//                output = ValidityEnum.GFD;
//            }
//            orderResponse.setValidity(output);
//        }
        if (o.getStatus().equalsIgnoreCase(OrderStatus.CAN.toString())) {
            orderResponse.setQuantity(o.getCancelSize());
        } else if (o.getStatus().equalsIgnoreCase(OrderStatus.TRAD.toString())) {
            orderResponse.setQuantity(o.getFilledShares());
        }
        orderResponse.setQuantity(o.getQuantity());
        if (o.getStatus() != null && !o.getStatus().equalsIgnoreCase(CharConstant.DASH)) {
            String input = o.getStatus();
            com.trade.algotrade.enums.OrderStatus output = null;
            if (input.equals("NEWF")) {
                output = com.trade.algotrade.enums.OrderStatus.OPEN;
            }
            orderResponse.setStatus(output);
        }

        return orderResponse;
    }

    private OrderResponse mapGetOrderResponse(KotakOrderDto o) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrderId(o.getOrderId());
        if (o.getTransactionType() != null && !StringUtils.isEmpty(o.getTransactionType())) {
            orderResponse.setTransactionType(TransactionType.fromValue(o.getTransactionType()));
        }
        orderResponse.setExpiryDate(o.getExpiryDate());
        orderResponse.setOrderTimestamp(o.getOrderTimestamp());
        if (o.getVariety() != null && !StringUtils.isEmpty(o.getVariety())) {
            orderResponse.setVariety(com.trade.algotrade.client.angelone.enums.VarietyEnum.fromValue(o.getVariety()));
        }
        orderResponse.setPrice(o.getPrice());
        if (o.getValidity() != null && !o.getValidity().equalsIgnoreCase(CharConstant.DASH)) {
            String input = o.getValidity();
            ValidityEnum output = null;
            if (input.equals("Good For Day")) {
                output = ValidityEnum.GFD;
            }
            orderResponse.setValidity(output);
        }
        if (o.getStatus().equalsIgnoreCase(OrderStatus.CAN.toString())) {
            orderResponse.setQuantity(o.getCancelledQuantity());
        } else if (o.getStatus().equalsIgnoreCase(OrderStatus.TRAD.toString())) {
            orderResponse.setQuantity(o.getFilledQuantity());
        }
        orderResponse.setQuantity(o.getOrderQuantity());
        if (o.getStatus() != null && !o.getStatus().equalsIgnoreCase(CharConstant.DASH)) {
            String input = o.getStatus();
            com.trade.algotrade.enums.OrderStatus output = null;
            if (input.equals("NEWF")) {
                output = com.trade.algotrade.enums.OrderStatus.OPEN;
            }
            orderResponse.setStatus(output);
        }

        return orderResponse;
    }


    private boolean isPreOrderValidationSuccess(UserResponse userDetails, OrderRequest orderRequest) {

        logger.debug("**** Pre Validating order with  User ID := {}  ****", userDetails.getUserId());

        if (orderRequest.getOrderCategory() == OrderCategoryEnum.SQAREOFF) {
            logger.info("**** Pre Validating order for SQUARE OFF Order with  User ID := {}  Order Request {} ****", userDetails.getUserId(), orderRequest.getOrderCategory());
            //return checkSquareOffCondition(orderRequest, trades);
            return true;
        } else {

            List<TradeEntity> todaysTrade = tradeService.getTodaysTradeByUserId(userDetails.getUserId());
            List<OpenOrderEntity> openOrdersTodayByUser = getTodaysOpenOrdersByUserId(userDetails.getUserId());
            if (CollectionUtils.isEmpty(todaysTrade) && CollectionUtils.isEmpty(openOrdersTodayByUser)) {
                logger.info("**** Pre Validating (NEW ORDER): SUCCESS :-  No Open Trade and No Open Order,Proceeding for place order for User ID := {}  ****", userDetails.getUserId());
                return true;
            }

            Supplier<Stream<TradeEntity>> tradeEntityStream = () -> todaysTrade.stream().filter(trade -> trade.getTradeStatus() == TradeStatus.OPEN);
            if (tradeEntityStream.get().findAny().isPresent()) {
                logger.info("**** Pre Validating (NEW ORDER): FAILED :-  Current Open Trade Exceed Condition, Open Trade Count ({}) for User ID := {}  ****", tradeEntityStream.get().count(), userDetails.getUserId());
                return false;
            }

            if (isMaximumTradesPerDayExceedsDefinedLimit(todaysTrade, userDetails, orderRequest)) {
                TradeUtils.getUserDayLimitExceeded().put(userDetails.getUserId(), "MAX TRADES PER DAY EXCEED");
                logger.info("**** Pre Validating (NEW ORDER): FAILED :-  Maximum trades per day Exceed Condition, Open Trade Count ({}) for User ID := {}  ****", todaysTrade.size(), userDetails.getUserId());
                return false;
            }

//            if (isMaximumLossPerDayExceedsDefinedLimit(userDetails, orderRequest.getSegment())) {
//                TradeUtils.getUserDayLimitExceeded().put(userDetails.getUserId(), "MAX LOSS PER DAY EXCEED");
//                logger.info("**** Pre Validating (NEW ORDER): FAILED :- Maximum loss per day condition exceed for User ID := {}  ****", userDetails.getUserId());
//                return false;
//            }
//
//            if (isProfitPerDayExceedDefinedLimit(todaysTrade, userDetails, orderRequest)) {
//                TradeUtils.getUserDayLimitExceeded().put(userDetails.getUserId(), "MAX PROFIT PER DAY EXCEED");
//                logger.info("**** Pre Validating (NEW ORDER): FAILED :- Maximum profit per day exceed condition for User ID := {}  ****", userDetails.getUserId());
//                return false;
//            }

            logger.debug("**** Pre Validating (NEW ORDER) No validation condition FAILED for User {} Hence returning SUCCESS  ****", userDetails.getUserId());
            return true;
        }
    }

    private boolean isMaximumTradesPerDayExceedsDefinedLimit(List<TradeEntity> todayTrade, UserResponse userDetails, OrderRequest orderRequest) {
        Integer maxAllowedTradesPerSegmentPerDay = userDetails.getMaxTradesPerDay().get(orderRequest.getSegment());
        int todayTrades = todayTrade.size();
        if (maxAllowedTradesPerSegmentPerDay <= todayTrades) {
            logger.info("**** Segment Trade Count := {} and Maximum allowed Trades  Per Segment Per Day := {} ****",
                    todayTrades, maxAllowedTradesPerSegmentPerDay);
            return true;
        }
        return false;
    }

    private boolean isProfitPerDayExceedDefinedLimit(List<TradeEntity> todaysTrade, UserResponse userDetails, OrderRequest orderRequest) {
        BigDecimal maxAllowedProfit = userDetails.getDayProfitLimit().get(orderRequest.getSegment());
        BigDecimal todaysRealisedPnl = tradeService.getTodaysRealisedPnl(userDetails.getUserId());
        if (todaysRealisedPnl == null || BigDecimal.ZERO.compareTo(todaysRealisedPnl) == 0) {
            return false;
        }
        return todaysRealisedPnl.compareTo(maxAllowedProfit) >= 0;
    }


    private boolean checkMarginAvailable(UserResponse userDetails, OrderRequest orderRequest) {
        /**
         * Checking user available margin with required Skip the check for mock orders
         */
        if (Boolean.TRUE.equals(userDetails.getIsRealTradingEnabled())) {
            BigDecimal marginRequired = orderRequest.getPrice().multiply(new BigDecimal(orderRequest.getQuantity()));
            BigDecimal availableMargin = marginService.getMargin(userDetails.getUserId());
            //If margin is null fetch margin for user from kotak
            if (availableMargin == null || availableMargin.compareTo(BigDecimal.ZERO) == 0) {
                MarginData marginData = marginService.getMarginFromKotak(userDetails.getUserId());
                if (Objects.isNull(marginData))
                    return true;
                marginService.updateMarginInDB(userDetails.getUserId(), new BigDecimal(marginData.getNet()));
                availableMargin = new BigDecimal(marginData.getNet());
            }
            if (availableMargin != null && marginRequired.compareTo(availableMargin) > 0) {
                logger.info(
                        "**** Required margin {} is higher than available margin {}, aborting place order request. ****",
                        marginRequired, availableMargin);
                return true;
            }
        }
        return false;
    }

    private boolean isMaximumLossPerDayExceedsDefinedLimit(UserResponse userDetails, String segment) {
        BigDecimal lossPerSegmentPerDay = userDetails.getDayLossLimit().get(segment);
        BigDecimal totalRealisedPnl = tradeService.getTodaysRealisedPnl(userDetails.getUserId());
        if (totalRealisedPnl == null || BigDecimal.ZERO.compareTo(totalRealisedPnl) == 0) {
            return false;
        }
        return totalRealisedPnl.compareTo(lossPerSegmentPerDay.negate()) <= 0;
    }

    private boolean checkSquareOffCondition(OrderRequest orderRequest, List<TradeEntity> trade) {
        if (CollectionUtils.isEmpty(trade)) {
            logger.info("**** No Open Trade found for Instrument and square-off Category ****");
            return false;
        }

/*        if (CollectionUtils.isEmpty(trade) && orderRequest.getTriggerType() == TriggerType.AUTO) {
            logger.info("**** No Open Trade found for Instrument {} and square-off Category  {} ****",
                    orderRequest.getInstrumentToken(), orderRequest.getOrderCategory().toString());
            return false;
        }*/
        if (trade.stream().findFirst().isPresent()) {
            logger.info("**** Open Trade found for Instrument {} and square-off Category  {} ****",
                    orderRequest.getInstrumentToken(), orderRequest.getOrderCategory().toString());
            if (orderRequest.getTransactionType() == TransactionType.SELL && orderRequest.getQuantity() <= trade
                    .stream().findFirst().get().getSellOpenQuantityToSquareOff()) {
                return true;
            } else if (orderRequest.getTransactionType() == TransactionType.BUY && orderRequest
                    .getQuantity() <= trade.stream().findFirst().get().getBuyOpenQuantityToSquareOff()) {
                return true;
            }
        }
        logger.info("**** Open Buy/Sell Quantity is less than Square-off order, Instrument := {} Quantity:= {} ****",
                orderRequest.getInstrumentToken(), orderRequest.getQuantity());
        return false;
    }

    /**
     * Iterate over users to get orders of all users ,
     * <p>
     * Get all orders of user specific from KOTAK client and merge into single list
     * of orders
     */

    @Override
    public List<OrderResponse> getOpenOrders() {
        return userService.getUsers().stream().map(u -> getAllOrders(u.getUserId())).reduce(new ArrayList<>(), (x, y) -> {
            x.addAll(y);
            return x;
        });
    }

    /**
     * Find All Orders with status EXECUTED in open_orders document,
     * <p>
     * If copy/save all those order to closed_orders document.
     * <p>
     * Delete all executed orders from open_orders document
     */
    @Override
    //  @CacheEvict(value = "openOrders", allEntries = true)
    public void moveOrdersToHistory(List<TradeHistoryEntity> tradeHistoryEntities) {

        tradeHistoryEntities.forEach(th -> {
            List<OpenOrderEntity> completedOrders = th.getOrders();
            List<ClosedOrderEntity> historyOrders = completedOrders.stream().map(o -> {
                ClosedOrderEntity orderHistory = ClosedOrderEntity.builder().build();
                BeanUtils.copyProperties(o, orderHistory);
                return orderHistory;
            }).collect(Collectors.toList());
            closedOrderRepository.saveAll(historyOrders);
            openOrderRepository.deleteAll(completedOrders);

        });
        /*
            moveNonCompletedTradeOrders();
         */
    }

    public Integer getOrderQuantityForStrategy(String userId, String strategyName) {
        UserStrategyEntity userStrategyEntity = userStrategyService.findStrategyByUserIdAndStrategy(userId,
                strategyName);
        Integer orderQuantity;
        StrategyResponse strategy = strategyService.getStrategy(strategyName);
        EntryCondition entryCondition = strategy.getEntryCondition();
        if (entryCondition == null) {
            throw new AlgotradeException(ErrorCodeConstants.ENTRY_CONDITION_NOT_FOUND);
        }
        Map<String, String> entryConditions = entryCondition.getConditions();
        if (CollectionUtils.isEmpty(entryConditions)) {
            throw new AlgotradeException(ErrorCodeConstants.ENTRY_CONDITION_NOT_FOUND);
        }
        if (userStrategyEntity != null) {
            orderQuantity = userStrategyEntity.getQuantity();
        } else {
            orderQuantity = Integer.valueOf(entryCondition.getConditions().get(Constants.DEFAULT_QUANTITY));
        }
        String lastOrderedQuantity = entryConditions.get(Constants.LAST_ORDER_QUANTITY);
        BigDecimal finalOrderQuantity;
        if (lastOrderedQuantity == null || Integer.parseInt(lastOrderedQuantity) == 0) {
            finalOrderQuantity = new BigDecimal(orderQuantity);
        } else {
            finalOrderQuantity = Constants.ONE_POINT_TWENTY_FIVE.multiply(BigDecimal.valueOf(Integer.parseInt(lastOrderedQuantity)));
        }
        return finalOrderQuantity.intValue();
    }

    @Override
    public List<OpenOrderEntity> getOpenOrdersByOrderIds(List<Long> orderIds) {
        return openOrderRepository.findByOrderIdIn(orderIds);
    }

    @Override
    // @CacheEvict(value = "openOrders", allEntries = true)
    public OpenOrderEntity saveOpenOrders(OpenOrderEntity openOrderEntity) {
        logger.debug("Saving Order with details  {}", openOrderEntity.getOrderId());
        return openOrderRepository.save(openOrderEntity);
    }

    // @CacheEvict(value = "openOrders", allEntries = true)
    public List<OpenOrderEntity> updateOpenOrders(List<OpenOrderEntity> openOrders) {
        return openOrderRepository.saveAll(openOrders);
    }

    @Override
    //  @Cacheable(value = "openOrders", key = "#userId")
    public List<OpenOrderEntity> getTodaysOpenOrdersByUserId(String userId) {
        return openOrderRepository.findByUserIdAndOrderStatusAndCreatedAtGreaterThanEqual(userId,
                com.trade.algotrade.enums.OrderStatus.OPEN, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    @Override
    public List<OpenOrderEntity> getTodaysOrdersByStatus(com.trade.algotrade.enums.OrderStatus orderStatus) {
        return openOrderRepository.findByOrderStatusAndCreatedAtGreaterThanEqual(
                orderStatus, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    @Override
    // @Cacheable(value = "openOrders", key = "#orderId")
    public OpenOrderEntity getOpenOrderByOrderId(Long orderId) {
        Optional<OpenOrderEntity> optionalOpenOrderEntity = openOrderRepository.findByOrderId(orderId);
        if (optionalOpenOrderEntity.isEmpty()) {
            throw new AlgoValidationException(ErrorCodeConstants.ORDER_NOT_FOUND_ERROR);
        }
        return optionalOpenOrderEntity.get();
    }

    @Override
    public List<OrderResponse> prepareOrderAndCreateOrder(OrderRequest orderRequest) {
        orderRequest.setProduct(com.trade.algotrade.client.angelone.enums.ProductType.INTRADAY);
        orderRequest.setTag(Constants.ALGOTRADE_TAG);
        orderRequest.setVariety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL);
        return createOrder(orderRequest);
    }

    @Override
    public OpenOrderEntity getOpenSquareOffOrderByInstrumentToken(Long instrumentToken) {
        List<OpenOrderEntity> openSquareOffOrders = openOrderRepository
                .findByOrderStatusAndOrderCategoryAndInstrumentToken(com.trade.algotrade.enums.OrderStatus.OPEN,
                        OrderCategoryEnum.SQAREOFF, instrumentToken);
        Optional<OpenOrderEntity> first = openSquareOffOrders.stream().findFirst();
        return first.orElse(null);
    }

    @Override
    public void cancelExpiryOrder() {
        List<OpenOrderEntity> openNewExpiryOrders = openOrderRepository.findByOrderStatusAndStrategyNameAndOrderCategory(com.trade.algotrade.enums.OrderStatus.OPEN, Strategy.EXPIRY.toString(), OrderCategoryEnum.NEW);
        if (CollectionUtils.isEmpty(openNewExpiryOrders)) {
            logger.info("[cancelExpiryOrder] No New Order Found for Expiry Strategy, Hence returning from scheduler");
            return;
        }
        openNewExpiryOrders.forEach(o -> {
            logger.info("[cancelExpiryOrder] Cancelling order with Order ID :={}", o.getOrderId());
            // Delete open new Order, Default SL order won't be placed as New order is not executed, So need to delete square-off orders here
            cancelOrder(o.getOrderId(), o.getUserId());
        });
    }

    @Override
    public void squareOffOrder(String strategyName) {

        // Only SL or Target order will find here
        List<OpenOrderEntity> openSquareOffOrders = openOrderRepository.findByOrderStatusAndStrategyNameAndOrderCategory(com.trade.algotrade.enums.OrderStatus.OPEN, strategyName, OrderCategoryEnum.SQAREOFF);
        if (CollectionUtils.isEmpty(openSquareOffOrders)) {
            logger.info("No open order found to Square off  for the Strategy := {}", strategyName);
            return;
        }

        logger.info("Total := {} {} strategy open orders found", openSquareOffOrders.size(), strategyName);
        openSquareOffOrders.forEach(o -> {
            //Square off all open Expiry positions
            Optional<OpenOrderEntity> optionalOpenOrderEntity = openOrderRepository.findByOrderId(o.getOriginalOrderId());
            if (optionalOpenOrderEntity.isEmpty()) {
                throw new AlgoValidationException(ErrorCodeConstants.ORDER_NOT_FOUND_ERROR);
            }
            if (optionalOpenOrderEntity.get().getOrderStatus() == com.trade.algotrade.enums.OrderStatus.EXECUTED && o.getOrderStatus() == com.trade.algotrade.enums.OrderStatus.OPEN) {
                modifySlOrderToMarketOrder(o, SLTriggerReasonEnum.INTRADAY_SQAREOFF.toString());
                logger.info("Order modified for the Order ID := {},Status := {},Order Type := {}", o.getOrderId(), o.getOrderStatus(), o.getOrderType());
            }
        });
    }

    @Override
    public boolean modifySlOrderToMarketOrder(OpenOrderEntity o, String squareOffReason) {
        o.getSlDetails().setTriggerPrice(o.getPrice());
        OrderRequest orderRequest = OrderRequest.builder()
                .instrumentToken(o.getInstrumentToken())
                .transactionType(o.getTransactionType())
                .quantity(o.getQuantity())
                .optionType(o.getOptionType())
                .orderType(OrderType.MARKET)
                .userId(o.getUserId())
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .product(com.trade.algotrade.client.angelone.enums.ProductType.INTRADAY)
                .orderCategory(o.getOrderCategory())
                .slDetails(o.getSlDetails())
                .orderId(o.getOrderId())
                .build();
        logger.info("Modifying Order {} With Data {}", o.getOrderId(), orderRequest);
        List<OrderResponse> orderResponses = modifyOrder(o.getOrderId(), orderRequest, squareOffReason, true);
        if (!orderResponses.isEmpty() && CommonUtils.getOffMarketHoursTestFlag()) {
            websocketUtils.subscribeKotakOrderWebsocketMock(o.getOrderId(), o.getUserId(), String.valueOf(o.getPrice()), "S");
        }
        return !CollectionUtils.isEmpty(orderResponses);
    }

    @Override
    public List<OpenOrderEntity> getTodaysAllOrdersByUserId(String userId) {
        return openOrderRepository.findByUserIdAndCreatedAtGreaterThanEqual(userId, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    @Override
    public List<OpenOrderEntity> getTodaysOrdersByCategory(OrderCategoryEnum orderCategoryEnum, boolean isSlOrderPlaced) {
        return openOrderRepository.findByOrderCategoryAndIsSlOrderPlacedAndCreatedAtGreaterThanEqual(orderCategoryEnum, isSlOrderPlaced, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    @Override
    public List<OrderResponse> getAllOrdersByFilter(OrderFilterQuery orderFilter) {
        List<OrderResponse> filteredOrders = new ArrayList<>();
        FilterBuilder filterBuilder = new FilterBuilder();
        if (Objects.nonNull(orderFilter.getUserId())) {
            filterBuilder.addFilter("userId", CriteriaFilter.eq, orderFilter.getUserId());
        }
        if (Objects.nonNull(orderFilter.getOrderStatus())) {
            filterBuilder.addFilter("orderStatus", CriteriaFilter.eq, orderFilter.getOrderStatus());
        }
        if (Objects.nonNull(orderFilter.getOrderType())) {
            filterBuilder.addFilter("orderType", CriteriaFilter.eq, orderFilter.getOrderType());
        }
        if (Objects.nonNull(orderFilter.getOrderCategory())) {
            filterBuilder.addFilter("orderCategory", CriteriaFilter.eq, orderFilter.getOrderCategory());
        }
        if (Objects.nonNull(orderFilter.getTransactionType())) {
            filterBuilder.addFilter("transactionType", CriteriaFilter.eq, orderFilter.getTransactionType());
        }
        if (Objects.nonNull(orderFilter.getSegment())) {
            filterBuilder.addFilter("segment", CriteriaFilter.eq, orderFilter.getSegment());
        }

        //For now this method will return either the active orders or history orders not all orders
        if (Objects.nonNull(orderFilter.getOrderState()) && orderFilter.getOrderState() == OrderState.HISTORY) {
            filteredOrders = closedOrderRepository.findAllByFiter(ClosedOrderEntity.class, filterBuilder).stream().map(this::mapOrderEntityToOrderResponse).collect(Collectors.toList());
        } else if (Objects.nonNull(orderFilter.getOrderState()) && orderFilter.getOrderState() == OrderState.ALL) {
            filteredOrders = openOrderRepository.findAllByFiter(OpenOrderEntity.class, filterBuilder).stream().map(this::mapOrderEntityToOrderResponse).collect(Collectors.toList());
            List<OrderResponse> historyOrders = closedOrderRepository.findAllByFiter(ClosedOrderEntity.class, filterBuilder).stream().map(this::mapOrderEntityToOrderResponse).collect(Collectors.toList());
            filteredOrders.addAll(historyOrders);
        } else {
            // Default It will return only active Orders
            filteredOrders = openOrderRepository.findAllByFiter(OpenOrderEntity.class, filterBuilder).stream().map(this::mapOrderEntityToOrderResponse).collect(Collectors.toList());
        }
        return filteredOrders;
    }

    private OrderResponse mapOrderEntityToOrderResponse(OrderEntity orderEntity) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setUserId(orderEntity.getUserId());
        orderResponse.setOrderId(orderEntity.getOrderId());
        orderResponse.setQuantity(orderEntity.getQuantity());
        orderResponse.setSquareOffReason(orderEntity.getSquareOffReason());
        orderResponse.setSlDetailsHistory(orderEntity.getSlDetailsHistory());
        orderResponse.setCreatedAt(orderEntity.getCreatedAt());
        orderResponse.setInstrumentToken(orderEntity.getInstrumentToken());
        orderResponse.setPrice(orderEntity.getPrice());
        orderResponse.setOptionType(orderEntity.getOptionType());
        orderResponse.setOrderStatus(orderEntity.getOrderStatus());
        orderResponse.setOrderType(orderEntity.getOrderType());
        orderResponse.setStrategyName(orderEntity.getStrategyName());
        orderResponse.setTransactionType(orderEntity.getTransactionType());
        orderResponse.setValidity(orderEntity.getValidity());
        orderResponse.setVariety(orderEntity.getVariety());
        orderResponse.setTradeId(orderEntity.getTradeId());
        orderResponse.setOrderCategory(orderEntity.getOrderCategory());
        orderResponse.setSlDetailsHistory(orderEntity.getSlDetailsHistory());
        orderResponse.setModificationCount(orderEntity.getModificationCount());
        orderResponse.setStrikePrice(orderEntity.getStrikePrice());
        orderResponse.setSegment(orderEntity.getSegment());
        orderResponse.setUpdatedAt(orderEntity.getUpdatedAt());
        return orderResponse;
    }

    @Override
    public List<OpenOrderEntity> getTodaysOrdersByInstrumentTokenAndStatus(Long instrumentToken, com.trade.algotrade.enums.OrderStatus orderStatus) {
        return openOrderRepository.findByInstrumentTokenAndOrderStatusAndCreatedAtGreaterThanEqual(instrumentToken, orderStatus, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    @Override
    public OpenOrderEntity findOrderByOrderId(Long orderId) {
        Optional<OpenOrderEntity> optionalOpenOrderEntity = openOrderRepository.findByOrderId(orderId);
        if (optionalOpenOrderEntity.isEmpty()) {
            throw new AlgoValidationException(ErrorCodeConstants.ORDER_NOT_FOUND_ERROR);
        }
        return optionalOpenOrderEntity.get();
    }

    @Override
    public List<OpenOrderEntity> getAllOpenSLOrdersByUserId(String userId) {
        return openOrderRepository.findByUserIdAndOrderCategoryAndOrderStatus(userId, OrderCategoryEnum.SQAREOFF, com.trade.algotrade.enums.OrderStatus.OPEN);
    }

    @Override
    public Boolean modifySLOrderStatusAndPrice(OpenOrderEntity orderEntity, String currentOrderStatus) {
        String tradeId = orderEntity.getTradeId();
        Optional<TradeEntity> tradeEntityOptional = tradeService.getTradeById(tradeId);
        if (tradeEntityOptional.isPresent() && tradeEntityOptional.get().getTradeStatus() != TradeStatus.COMPLETED && currentOrderStatus.equalsIgnoreCase(OrderStatus.TRAD.toString())) {
            TradeEntity tradeEntity = tradeEntityOptional.get();
            Optional<OpenOrderEntity> originalOrderOptional = openOrderRepository.findByOrderId(orderEntity.getOrderId());
            if (originalOrderOptional.isPresent()) {
                completeSLExecutedTrade(orderEntity, tradeEntity, originalOrderOptional);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public OpenOrderEntity getSLOrderByOriginalOrderId(Long originalOrderId) {
        logger.debug("Find Order With original order id  {}", originalOrderId);
        return openOrderRepository.findByOriginalOrderIdAndTransactionType(originalOrderId, TransactionType.SELL).orElse(null);
    }

    @Override
    public List<OpenOrderEntity> getOrdersByIsSLPlaced(boolean flag) {
        logger.debug("Get Order By SL FLAG  {}", flag);
        return openOrderRepository.findByIsSlOrderPlacedAndCreatedAtGreaterThanEqual(flag, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    private void completeSLExecutedTrade(OpenOrderEntity orderEntity, TradeEntity tradeEntity, Optional<OpenOrderEntity> originalOrderOptional) {
        OpenOrderEntity originalOrder = originalOrderOptional.get();
        BigDecimal buyTotal = originalOrder.getPrice().multiply(new BigDecimal(originalOrder.getQuantity()));
        BigDecimal squareOffPrice = orderEntity.getPrice().multiply(new BigDecimal(orderEntity.getQuantity()));
        BigDecimal realisedPnl = squareOffPrice.subtract(buyTotal);
        tradeEntity.setRealisedPnl(tradeEntity.getRealisedPnl().add(realisedPnl));
        if (tradeEntity.getRealisedPnl().compareTo(BigDecimal.ZERO) >= 0) {
            tradeEntity.setSuccessStatus(Constants.TRADE_SUCCESSFUL);
        } else {
            tradeEntity.setSuccessStatus(Constants.TRADE_UNSUCCESSFUL);
        }
        tradeEntity.setUpdatedTime(DateUtils.getCurrentDateTimeIst());
        tradeService.modifyTrade(tradeEntity);
        openOrderRepository.save(orderEntity);
        BigDecimal updatedMargin = marginService.getMargin(orderEntity.getUserId()).subtract(orderEntity.getPrice().multiply(new BigDecimal(orderEntity.getQuantity())));
        marginService.updateMarginInDB(orderEntity.getUserId(), updatedMargin);
    }
}

