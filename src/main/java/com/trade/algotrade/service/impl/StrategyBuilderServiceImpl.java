package com.trade.algotrade.service.impl;

import com.trade.algotrade.client.angelone.AngelOneClient;
import com.trade.algotrade.client.angelone.enums.OrderStatus;
import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.client.angelone.enums.*;
import com.trade.algotrade.client.angelone.request.GetCandleRequest;
import com.trade.algotrade.client.angelone.response.feed.QouteResponse;
import com.trade.algotrade.client.angelone.service.AngelCandleService;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.client.kotak.request.TargetDetails;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.dto.ExitCondition;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.dto.websocket.OrderFeedWSMessage;
import com.trade.algotrade.enitiy.*;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.exceptions.*;
import com.trade.algotrade.repo.InstrumentWatchMasterRepo;
import com.trade.algotrade.request.ManualOrderRequest;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.StrategyResponse;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.*;
import com.trade.algotrade.sort.InstrumentStrikePriceComparator;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import com.trade.algotrade.utils.WebsocketUtils;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
public class StrategyBuilderServiceImpl implements StrategyBuilderService {

    private final Logger logger = LoggerFactory.getLogger(StrategyBuilderServiceImpl.class);

    @Autowired
    InstrumentService instrumentService;

    @Autowired
    OrderService orderService;

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    StrategyService strategyService;

    @Autowired
    InstrumentStrikePriceComparator instrumentStrikePriceComparator;

    @Autowired
    CandleService candleService;

    @Autowired
    AngelCandleService angelCandleService;

    @Autowired
    MarginService marginService;

    @Autowired
    WebsocketUtils websocketUtils;

    @Autowired
    InstrumentWatchMasterRepo instrumentWatchMasterRepo;

    @Autowired
    UserStrategyService userStrategyService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    AngelOneClient angelOneClient;

    public AngelOneInstrumentMasterEntity instrumentForOrder = null;

    public static boolean isMonitorInstrumentSubscribed = false;

    @Override
    public void buildBigCandleStrategy(LiveFeedWSMessage webSocketMessage, Strategy strategyName) {
        logger.debug("buildStrategy Called : Strategy {} Live feed {}", strategyName, webSocketMessage);
        StrategyResponse strategy;
        try {
            strategy = strategyService.getStrategy(strategyName.toString());
        } catch (AlgotradeException algotradeException) {
            // Get the strategy configurations from DB for the input strategy, If input strategy not found in config then don't proceed.
            logger.info("No Configuration found for the strategy := {} , Error details: {} Hence Returning.", strategyName, algotradeException.getError());
            return;
        }

        List<CandleEntity> dayCandles;
        Collection<CandleEntity> values = TradeUtils.getNiftyBankCandle().values();
        //TODO  Get only latest 15 Min Candle data direct sorted from DB if its coming from DB
        if (CollectionUtils.isEmpty(values)) {
            values = candleService.findCandlesBySymbolsDescending(Constants.BANK_NIFTY_INDEX);
        }
        if (CollectionUtils.isEmpty(values)) {
            logger.info("TradeUtils.getNiftyBankCandle() Found Empty {} , Hence Returning", values);
            return;
        }
        // Sorting candle by time in ascending order
        Comparator<? super CandleEntity> comparator = Comparator.comparing(CandleEntity::getStartTime);

        // Find All Today's Candle and sort it according to Start time ascending order
        dayCandles = values.stream()
                .filter(v -> v.getStartTime().isAfter(DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME))))
                .sorted(comparator).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(TradeUtils.trendSet) && !isMonitorInstrumentSubscribed) {
            instrumentForOrder = findInstrumentForOrder(dayCandles, webSocketMessage);
            if (Objects.isNull(instrumentForOrder)) {
                resetTrendFinderFlags();
                return;
            }
        }

        BigDecimal trendValue = candleService.latestTrendFinder();
        BigDecimal bigCandlePoints = getBigCandlePoints(strategy);
        if (Objects.isNull(trendValue) || trendValue.compareTo(bigCandlePoints) < 0) {
            logger.debug("Big candle latest trend finder is null or less than big candle points, hence returning.");
            return;
        }


        int lastXTickValues = 3;
        if (CollectionUtils.isEmpty(TradeUtils.trendSet) || TradeUtils.trendSet.size() <= lastXTickValues) {
            logger.info("TREND SET DON'T HAVE {} MIN VALUES, HENCE NOT PROCEEDING FOR ORDER ", lastXTickValues);
            candleService.clearTrendData();
            CommonUtils.pause(60000);
            return;
        }
        List<BigDecimal> recentTrendDiff = TradeUtils.trendSet.stream().collect(Collectors.toList()).subList(Math.max(TradeUtils.trendSet.size() - lastXTickValues, 0), TradeUtils.trendSet.size());
        BigDecimal maxDifference = recentTrendDiff.stream().sorted().max(BigDecimal::compareTo).get();
        int maxTickDifference = 7;
        if (maxDifference.compareTo(new BigDecimal(maxTickDifference)) > 0) {
            logger.info("TREND IS VOLATILE, AS MAX TICK GAP IS {}, HENCE NOT PROCEEDING FOR ORDER ", maxTickDifference);
            candleService.clearTrendData();
            CommonUtils.pause(60000);
            return;
        }

        if (DateUtils.isEndOrStartOfCandle()) {
            logger.info("Start or end of candle, hence not placing order");
            candleService.clearTrendData();
            CommonUtils.pause(60000);
            return;
        }

        logger.info("TREND TICK DIFFERENCE {}, LAST {} UNIQUE VALUES ARE {}, PROCEEDING FOR ORDER", maxDifference, lastXTickValues, recentTrendDiff);
        if (commonUtils.isTodayExpiryDay() && DateUtils.isExpiryAfternoonTime() && !CommonUtils.getOffMarketHoursTestFlag()) {
            logger.debug("Returning from Big Candle processing flow Because,On Expiry Day trade is allowed after 12 PM IST for strategy := {} *****", strategyName);
            return;
        }
        synchronized (this) {
            processBigCandleStrategy(instrumentForOrder, strategy);
        }

    }

    private AngelOneInstrumentMasterEntity findInstrumentForOrder(List<CandleEntity> dayCandles, LiveFeedWSMessage webSocketMessage) {
        CandleEntity lastCandle;
        lastCandle = dayCandles.get(dayCandles.size() - 1);
        if (lastCandle.getOpen().compareTo(lastCandle.getClose()) == 0) {
            logger.info("Invalid last candle : {}, hence returning.. ", lastCandle);
            return null;
        }
        logger.debug("last candle := {} live feed ltp := {}", lastCandle, webSocketMessage.getLtp());
        OptionType bigCandleOptionType = OptionType.CE;
        Optional<AngelOneInstrumentMasterEntity> instrument;
        List<Integer> tradeStrikePrice = TradeUtils.getClosestStrikePrices(webSocketMessage.getLtp());

        if (TradeUtils.isPlus(lastCandle.getOpen(), webSocketMessage.getLtp())) {
            instrument = instrumentService.getAllInstruments().stream().filter(i -> tradeStrikePrice.get(1).equals(i.getStrike()) && OptionType.CE.toString().equals(i.getOptionType())).findAny();
        } else {
            instrument = instrumentService.getAllInstruments().stream().filter(i -> tradeStrikePrice.get(0).equals(i.getStrike()) && OptionType.PE.toString().equals(i.getOptionType())).findAny();
            bigCandleOptionType = OptionType.PE;
        }
        if (instrument.isEmpty()) {
            logger.info("Empty Instrument Found for Instrument {} ,Hence Returning ****", instrument);
            return null;
        }
        AngelOneInstrumentMasterEntity kotakInstrumentMasterEntity = instrument.get();
        kotakInstrumentMasterEntity.setOptionType(bigCandleOptionType.name());
        return instrument.get();
    }

    private void processBigCandleStrategy(AngelOneInstrumentMasterEntity instruementMaster, StrategyResponse strategy) {
        logger.debug("BIG_CANDLE_STRATEGY FLOW  CALLED.");

        //TODO Convert to single user flow : Get only active user

        // check very first is there any order open for BIG-CANDLE Strategy if yes return from here.
        List<UserResponse> allFnoActiveUsers = userService.getAllActiveSegmentEnabledUsers(Segment.FNO);
        if (CollectionUtils.isEmpty(allFnoActiveUsers)) {
            logger.info("No Active user found for FNO segment to process {} strategy order,Hence Returning ", strategy);
            return;
        }
        UserResponse userResponse = allFnoActiveUsers.get(0);
        List<TradeEntity> allOpenTrades = tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(userResponse.getUserId(), instruementMaster.getInstrumentToken(), strategy.getStrategyName());
        // As we are checking trades are available for all the active users, Need to process order for those users only for whom orders not exists
        if (!CollectionUtils.isEmpty(allOpenTrades)) {
            // This condition satisfied means for all the users open trades are present no need to proceed with this order creation flow
            logger.info("Strategy :=  {} Open Trades (Count := {}) Found, Hence Returning ", strategy.getStrategyName(), allOpenTrades.size());
            return;
        }

        if (TradeUtils.isBigCandleProcessStart) {
            logger.info("Big candle order process in progress.");
            return;
        }

        BigDecimal bigCandlePoints = getBigCandlePoints(strategy);
        BigDecimal trendValue = candleService.latestTrendFinder();
        if (Objects.nonNull(trendValue) && trendValue.compareTo(bigCandlePoints) >= 0) {
            if (CommonUtils.getOffMarketHoursTestFlag()) {
                CommonUtils.pauseMockWebsocket = false;
            }

            TradeUtils.isBigCandleProcessStart = true;
            TradeUtils.isLatestTrendFinderActive = false;
            candleService.clearTrendData();

            OptionType bigCandleOptionType = OptionType.valueOf(instruementMaster.getOptionType());
            logger.info("Strike Price := {} and  Instrument token := {} Selected to buy := {},Price := {}, Trend value:= {}", instruementMaster.getStrike(), instruementMaster.getInstrumentToken(), bigCandleOptionType, instruementMaster.getLastPrice(), trendValue);
            List<OrderResponse> createdOrders = processBigCandleStrategyOrder(userResponse.getUserId(), instruementMaster, bigCandleOptionType, strategy);
            if (CollectionUtils.isEmpty(createdOrders)) {
                resetTrendFinderFlags();
//                createdOrders.forEach(orderResponse -> {
//                    if (orderResponse.getOrderType().equals(OrderType.LIMIT)) {
//                        checkLimitOrderIsOpen(orderResponse.getOrderId());
//                    }
//                });
            } else {
                logger.info("Processing Order feed for the price update and SL placement Total Orders  {}", createdOrders.size());
                processOrderFeed();
                // BUY ORDER complete order status feed will be processed here
                //If in case SL is not placed then place SL order after 2 minutes.
//                createdOrders.forEach(response -> {
//                    runSLExecuteVerificationThread(response);
//                });
            }
        }
    }

    @Override
    public void processOrderFeed() {
        OrderFeedWSMessage poll = TradeUtils.orderFeedQueue.poll();
        if (Objects.isNull(poll)) {
            int retryCount = 0;
            while (TradeUtils.orderFeedQueue.isEmpty()) {
                logger.error("THERE IS NO ORDER FEED FOUND IN QUEUE SIZE {}, Hence Waiting to receive order Feed.", TradeUtils.orderFeedQueue.size());
                try {
                    Thread.sleep(100);
                    retryCount++;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (retryCount > 10) {
                    break;
                }
            }
            if (TradeUtils.orderFeedQueue.isEmpty()) {
                logger.error("ORDER FEED NOT FOUND :  FAILED RETURNING WITHOUT PLACING SL", TradeUtils.orderFeedQueue.size());
                // TODO Fallback logic can be added here
                return;
            }
        }
        logger.info("Processing Order Feed for ORDER ID {} and Order Status {}, Object {}", poll.getOrderId(), poll.getStatus(), poll);
        try {
            addSLAndMarginForPlacedOrder(poll);
        } catch (Exception e) {
            logger.error("USER ID {} ORDER ID {} --> ORDER FEED --> EXCEPTION OCCURRED WHILE PROCESSING ORDER FEED MESSAGE -> for the Order Status {} , Message Source {}, Exception {}", poll.getUserId(), poll.getOrderId(), poll.getStatus(), poll.getMessageSource(), e.getStackTrace());
        }
        logger.info("Order Feed processed successfully for order ID {} and Quantity {}", poll.getOrderId(), poll.getQuantity());
    }

    private void resetTrendFinderFlags() {
        logger.info("Resetting trend flags");
        TradeUtils.isBigCandleProcessStart = false;
        TradeUtils.isLatestTrendFinderActive = true;
    }

    private boolean verifyConsecutiveSuccessOrders(String userId) {
        boolean isFailOrderAfterConsecutiveSuccessOrders = false;
        if (Objects.nonNull(TradeUtils.failureAfterConsecutiveSuccess.get(userId)) && TradeUtils.failureAfterConsecutiveSuccess.get(userId) == Boolean.TRUE) {
            logger.info("After some consecutive success orders there is a unsuccessful order hence not placing new order.");
            isFailOrderAfterConsecutiveSuccessOrders = true;
        }
        return isFailOrderAfterConsecutiveSuccessOrders;
    }


    private BigDecimal getBigCandlePoints(StrategyResponse strategy) {
        Map<String, String> bigCandleConditions = strategy.getEntryCondition().getConditions();
        String bigCandlePointsStr = bigCandleConditions.get(Constants.BIG_CANDLE_POINTS);
        return new BigDecimal(bigCandlePointsStr);
    }

    private void processExpiryStrategy(List<CandleEntity> dayCandles, CandleEntity lastCandle, Strategy strategyName) {

        // Sort Candles based on High based on Ascending
        Comparator<? super CandleEntity> candleHighComparator = Comparator.comparing(CandleEntity::getHigh);

        // Sort Candles based on Low Ascending
        Comparator<? super CandleEntity> candleLowComparator = Comparator.comparing(CandleEntity::getLow);

        // Get first candle of day as firstCandle of nifty bank
        CandleEntity firstCandle = dayCandles.get(0);

        // Find day Highest ltp of nifty bank
        BigDecimal dayHigh = findDayHigh(dayCandles, candleHighComparator);

        // Find day lowest ltp of nifty bank
        BigDecimal dayLow = findDayLow(dayCandles, candleLowComparator);

        // Find first half high of nifty bank
        BigDecimal firstHalfHigh = findFirstHalfHigh(dayCandles, candleHighComparator);

        // Find first half Low Ltp
        BigDecimal firstHalfLow = findFirstHalfLow(dayCandles, candleLowComparator);

        // Find Second half High Ltp
        BigDecimal secondHalfHigh = findSecondHalfHigh(dayCandles, candleHighComparator);

        // Find Second half Low Ltp
        BigDecimal secondHalfLow = findSecondHalfLow(dayCandles, candleLowComparator);

        // Find Current BANK NIFTY LTP
        BigDecimal currentMarket = lastCandle.getLtp();
        // BigDecimal dayMarketRange = dayHigh.subtract(dayLow);

        logger.info("**** [Expiry strategy] - Today's ltp values Highest : {}, Lowest : {}, First half high : {}, " + "First half low : {}, Second half high : {}, Second half low : {}, Current Market : {} ", dayHigh, dayLow, firstHalfHigh, firstHalfLow, secondHalfHigh, secondHalfLow, currentMarket);

        /*

          ltp values Highest : 44079.2000, Lowest : 43722.5500, First half high : 0,
          First half low : 0, Second half high : 44079.2000, Second half low :
          43722.5500, Current market : 43748.2000

         */
        OptionType optionType = null;

        if (firstCandle.getLow().compareTo(dayLow) == 0 && (firstCandle.getOpen().compareTo(currentMarket) <= 0 || firstHalfHigh.compareTo(secondHalfHigh) <= 0)) {
            optionType = OptionType.CE;
            logger.info("**** [Expiry strategy] - Market is in up trend ****");
            /*-
             * Trend is up : if
             * 1. firstCandle Low and The low of day is same
             * 2. Current LTP is Greater than First Candle Open.
             * 3. Second Half price is higher than First Half High price
             * 4.
             */

        } else if (firstCandle.getHigh().compareTo(dayHigh) == 0 && firstCandle.getOpen().compareTo(currentMarket) >= 0 || firstHalfLow.compareTo(secondHalfLow) >= 0) { // TREND DOWN
            optionType = OptionType.PE;
            logger.info("**** [Expiry strategy] - Market is in down trend ****");
            /*-
             * Trend DOWN up : if
             * 1. firstCandle High is the high of Day.
             * 2. Current LTP is Less than First Candle Open.
             * 3. Second Half Low is lower than First Low High
             */
        }

        if (dayHigh.subtract(firstCandle.getOpen()).abs().compareTo(new BigDecimal(400)) >= 0) {
            // Returning from here because nifty bank having good enough movement, fewer chances will be there in case of last 30 min
            return;
        }
        if (optionType == null) {
            logger.info("**** [Expiry strategy] - Unable to identify trend Considering Current Market and Day High,Day,Low ... ****");
            if (dayHigh.compareTo(currentMarket) <= 0) {
                optionType = OptionType.PE;

            } else if (dayLow.compareTo(currentMarket) <= 0) {
                optionType = OptionType.CE;
            }
            /*-
             * Sideways Market :
             * IF
             * 1. Condition TREND UP condition is not satisfied.
             * 2. Trend DOWN condition is not satisfied
             *
             * It means market is sideways
             *
             * E.g. Market Opens :
             * FIST CANDLE OPENS : 42220
             * FIRST CANDLE CLOSE : 42271
             * FIRST CANDLE LOW : 42120
             * FIRST CANDLE HIGH: 42295
             *
             * DAY HIGH: 42378
             * DAY LOW : 42107
             *
             * FIRST HALF HIGH : 42373
             * FIRST HALF LOW  : 42120
             *
             * SECOND HALF HIGH : 42294
             * SECOND HALF LOW : 42107
             *
             *
             */
        }
        if (optionType == null) {
            logger.info("**** [Expiry strategy] - Unable to identify trend hence cancelling Expiry Strategy ****");
            return;
        }
        List<Integer> expiryClosestStrikePrices = getExpiryClosestStrikePrices(lastCandle.getLtp());
        // Find appropriate Strike price.
        logger.info("**** [Expiry strategy] - Closest Strike Prices := {} ****", expiryClosestStrikePrices);
        List<AngelOneInstrumentMasterEntity> instruments = instrumentService.getAllInstruments().stream().filter(i -> expiryClosestStrikePrices.contains(i.getStrike())).collect(Collectors.toList());

        // Get the instrument token of selected Strike price
        logger.info("**** [Expiry strategy] - instruments : {} ****", instruments);
        Long instrumentToken = null;
        if (optionType == OptionType.PE) {
            // Pick Smallest strike price for the Order strike price in PE
            Optional<AngelOneInstrumentMasterEntity> lowestStrikePrice = instruments.stream().min(instrumentStrikePriceComparator);
            if (lowestStrikePrice.isPresent()) {
                instrumentToken = lowestStrikePrice.get().getInstrumentToken();
            }
            logger.info("**** [Expiry strategy] - Preparing PUT order for instrument {} ****", instrumentToken);
        } else {
            // Pick Biggest strike price for the Order strike price. IN CE
            Optional<AngelOneInstrumentMasterEntity> highestStrikePrice = instruments.stream().max(instrumentStrikePriceComparator);
            if (highestStrikePrice.isPresent()) {
                instrumentToken = highestStrikePrice.get().getInstrumentToken();
            }
            logger.info("**** [Expiry strategy] - Preparing CALL order for instrument {} ****", instrumentToken);
        }

        if (instrumentToken == null || instrumentToken == 0) {
            logger.info("**** [Expiry strategy] - instrumentToken Empty or Zero found,Hence returning from flow ****");
            return;
        }

        prepareExpiryOrder(optionType, instrumentToken, strategyName);
    }

    private void processExpiryScalpStrategy(CandleEntity lastCandle, Strategy strategy) {
        BigDecimal currentMarket = lastCandle.getLtp();

        logger.info("**** [Expiry Scalping strategy] - Current Market Price := {}", currentMarket);
        List<Integer> expiryClosestStrikePrices = getExpiryClosestStrikePrices(currentMarket);
        logger.info("**** [Expiry Scalping strategy] - expiryClosestStrikePrices : {} ****", expiryClosestStrikePrices);

        List<AngelOneInstrumentMasterEntity> instruments = instrumentService.getAllInstruments().stream().filter(i -> expiryClosestStrikePrices.contains(i.getStrike())).collect(Collectors.toList());

        logger.info("**** [Expiry Scalping strategy] - instruments : {} ****", instruments);

        boolean isCallDirectoin = TradeUtils.scalpDirecton.compareTo(BigDecimal.ZERO) > 0;
        OptionType optionType;
        Long instrumentToken = null;
        if (isCallDirectoin) {
            optionType = OptionType.CE;
            // Pick Biggest strike price for the Order strike price. IN CE
            Optional<AngelOneInstrumentMasterEntity> maxOptional = instruments.stream().max(instrumentStrikePriceComparator);
            if (maxOptional.isPresent()) {
                instrumentToken = maxOptional.get().getInstrumentToken();
                logger.info("**** [Expiry Scalping strategy] - Preparing CALL order for instrument {} ****", instrumentToken);
            }
        } else {
            optionType = OptionType.PE;
            // Pick Smallest strike price for the Order strike price in PE
            Optional<AngelOneInstrumentMasterEntity> minOptional = instruments.stream().min(instrumentStrikePriceComparator);
            if (minOptional.isPresent()) {
                instrumentToken = minOptional.get().getInstrumentToken();
                logger.info("**** [Expiry Scalping strategy] - Preparing PUT order for instrument {} ****", instrumentToken);
            }
        }

        if (instrumentToken == null || instrumentToken == 0) {
            logger.info("**** [Expiry Scalping strategy] - instrumentToken Empty or Zero found  Hence returning ****");
            return;
        }

        prepareExpiryOrder(optionType, instrumentToken, strategy);
    }

    private void processScalpingStrategy(CandleEntity lastCandle, OptionType optionType, Strategy strategyName) {
        BigDecimal currentMarket = lastCandle.getLtp();

        logger.info("**** [Expiry Scalping strategy] - Current market Price := {}", currentMarket);
        List<Integer> expiryClosestStrikePrices = getExpiryClosestStrikePrices(currentMarket);
        logger.info("**** [ Scalping strategy] - expiryClosestStrikePrices : {} ****", expiryClosestStrikePrices);

        List<AngelOneInstrumentMasterEntity> instruments = instrumentService.getAllInstruments().stream().filter(i -> expiryClosestStrikePrices.contains(i.getStrike())).collect(Collectors.toList());

        logger.info("**** [ Scalping strategy] - instruments : {} ****", instruments);

        Long instrumentToken = null;
        if (optionType == OptionType.CE) {
            // Pick Biggest strike price for the Order strike price. IN CE
            Optional<AngelOneInstrumentMasterEntity> maxOptional = instruments.stream().max(instrumentStrikePriceComparator);
            if (maxOptional.isPresent()) {
                instrumentToken = maxOptional.get().getInstrumentToken();
                logger.info("**** [Scalping strategy] - Preparing CALL order for instrument {} ****", instrumentToken);
            }
        } else {
            // Pick Smallest strike price for the Order strike price in PE
            Optional<AngelOneInstrumentMasterEntity> minOptional = instruments.stream().min(instrumentStrikePriceComparator);
            if (minOptional.isPresent()) {
                instrumentToken = minOptional.get().getInstrumentToken();
            }
            logger.info("**** [Scalping strategy] - Preparing PUT order for instrument {} ****", instrumentToken);
        }

        if (instrumentToken == null || instrumentToken == 0) {
            logger.info("**** [Scalping strategy] - instrumentToken Empty or Zero found  Hence returning ****");
            return;
        }

        prepareScalpOrder(optionType, instrumentToken, strategyName);
    }

    private void prepareExpiryOrder(OptionType optionType, Long instrumentToken, Strategy strategyName) {
        logger.info(" ****** [prepareExpiryOrder ] Called  ******** optionType {}, instrumentToken {}", optionType, instrumentToken);

        // Define SL details
        SlDetails slDetails = new SlDetails();
        slDetails.setTrailSl(true);
        slDetails.setSpread(BigDecimal.TEN);
        BigDecimal buyPrice = new BigDecimal("9.5");
        slDetails.setTriggerPrice(buyPrice.subtract(TradeUtils.getXPercentOfY(Constants.DEFAULT_SL_FIFTY_PERCENT, buyPrice)));
        slDetails.setSlPercent(Constants.DEFAULT_SL_FIFTY_PERCENT);

        // Define Target details
        TargetDetails targetDetails = new TargetDetails();
        targetDetails.setTargetPercent(Constants.THREE_HUNDRED_PERCENT);
        targetDetails.setTargetPrice(buyPrice.add(TradeUtils.getXPercentOfY(Constants.THREE_HUNDRED_PERCENT, buyPrice)));

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setTargetDetails(targetDetails);
        orderRequest.setOptionType(optionType);
        orderRequest.setPrice(buyPrice);
        orderRequest.setInstrumentToken(instrumentToken);
        orderRequest.setOrderType(OrderType.LIMIT);
        orderRequest.setSlDetails(slDetails);
        orderRequest.setStrategyName(strategyName.toString());
        orderRequest.setOrderCategory(OrderCategoryEnum.NEW);
        orderRequest.setSegment(Segment.FNO.toString());
        orderRequest.setTransactionType(TransactionType.BUY);
        orderService.prepareOrderAndCreateOrder(orderRequest);

        logger.info(" ****** [prepareExpiryOrder ] Completed  ******** optionType {}, instrumentToken {}", optionType, instrumentToken);
    }

    private void prepareScalpOrder(OptionType optionType, Long instrumentToken, Strategy strategyName) {
        logger.info(" ****** [prepareScalpOrder ] Called  ******** optionType {}, instrumentToken {}", optionType, instrumentToken);
        SlDetails slDetails = new SlDetails();
        slDetails.setTrailSl(true);
        slDetails.setSpread(BigDecimal.TEN);
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOptionType(optionType);
        orderRequest.setInstrumentToken(instrumentToken);
        orderRequest.setOrderType(OrderType.MARKET);
        orderRequest.setSlDetails(slDetails);
        orderRequest.setStrategyName(strategyName.toString());
        StrategyResponse strategyResponse = strategyService.getStrategy(strategyName.toString());
        String defaultQuantityString = strategyResponse.getEntryCondition().getConditions().get(Constants.DEFAULT_QUANTITY);
        if (StringUtils.isEmpty(defaultQuantityString)) {
            throw new AlgotradeException(ErrorCodeConstants.NOT_FOUND_DEFAULT_QUANTITY);
        }
        int quantity = Integer.parseInt(defaultQuantityString);
        orderRequest.setQuantity(quantity);
        orderRequest.setTransactionType(TransactionType.BUY);
        orderService.prepareOrderAndCreateOrder(orderRequest);
        logger.info(" ****** [prepareScalpOrder ] Completed  ******** optionType {}, instrumentToken {}", optionType, instrumentToken);
    }

    private BigDecimal findDayHigh(List<CandleEntity> dayCandles, Comparator<? super CandleEntity> highComparator) {
        BigDecimal high = BigDecimal.ZERO;
        Optional<CandleEntity> max = dayCandles.stream().max(highComparator);
        if (max.isPresent()) {
            high = max.get().getHigh();
        }
        return high;
    }

    private BigDecimal findFirstHalfHigh(List<CandleEntity> dayCandles, Comparator<? super CandleEntity> highComparator) {
        BigDecimal high = BigDecimal.ZERO;
        Optional<CandleEntity> max = dayCandles.stream().filter(c -> c.getStartTime().isBefore(DateUtils.marketSecondHalfStartDateTime())).max(highComparator);
        if (max.isPresent()) {
            high = max.get().getHigh();
        }
        return high;
    }

    private BigDecimal findSecondHalfHigh(List<CandleEntity> dayCandles, Comparator<? super CandleEntity> highComparator) {
        BigDecimal high = BigDecimal.ZERO;
        Optional<CandleEntity> max = dayCandles.stream().filter(c -> c.getStartTime().isAfter(DateUtils.marketSecondHalfStartDateTime())).max(highComparator);
        if (max.isPresent()) {
            high = max.get().getHigh();
        }
        return high;
    }

    private BigDecimal findDayLow(List<CandleEntity> dayCandles, Comparator<? super CandleEntity> lowComparator) {
        BigDecimal low = BigDecimal.ZERO;
        Optional<CandleEntity> min = dayCandles.stream().min(lowComparator);
        if (min.isPresent()) {
            low = min.get().getLow();
        }
        return low;
    }

    private BigDecimal findFirstHalfLow(List<CandleEntity> dayCandles, Comparator<? super CandleEntity> lowComparator) {
        BigDecimal low = BigDecimal.ZERO;
        Optional<CandleEntity> min = dayCandles.stream().filter(c -> c.getStartTime().isBefore(DateUtils.marketSecondHalfStartDateTime())).min(lowComparator);
        if (min.isPresent()) {
            low = min.get().getLow();
        }
        return low;
    }

    private BigDecimal findSecondHalfLow(Collection<CandleEntity> values, Comparator<? super CandleEntity> lowComparator) {
        BigDecimal low = BigDecimal.ZERO;
        Optional<CandleEntity> min = values.stream().filter(v -> v.getStartTime().isAfter(DateUtils.marketSecondHalfStartDateTime())).min(lowComparator);
        if (min.isPresent()) {
            low = min.get().getLow();
        }
        return low;
    }


    private List<Integer> getExpiryClosestStrikePrices(BigDecimal spotPrice) {

        int closesStrikePrice;
        int niftyBankSpotPrice = spotPrice.intValue();
        List<Integer> strikePrices = new ArrayList<>();

        /*- Find Nearest STRIKE PRICE OF SPOT PRICE
         *
         * E.G.  42230 SPOT PRICE
         * REMINDER : 30
         * QUOTIENT : 422
         *
         *
         * E.g. 2
         *
         * SPOT PRICE: 42270
         * REMINDER : 70
         * QOTIENT : 422
         *
         */
        int reminder = niftyBankSpotPrice % 100;
        int quotient = niftyBankSpotPrice / 100;

        if (reminder > 50) {
            closesStrikePrice = quotient * 100;
            /*-
             * Adding strike price of both side of closest spot price
             */
            closesStrikePrice = closesStrikePrice + 100;
            strikePrices.add(closesStrikePrice - 100);
            strikePrices.add(closesStrikePrice);
            strikePrices.add(closesStrikePrice + 100);
        }
        if (reminder <= 50) {
            /*-
             *  THIS CONDITION WILL BE SATISFIED HERE
             */
            closesStrikePrice = quotient * 100; // strikePrice base = 42200
            strikePrices.add(closesStrikePrice + 100); // next StrikePrice 42200 + 100 = 42300
            strikePrices.add(closesStrikePrice);
            strikePrices.add(closesStrikePrice - 100);
        }
        /*-
         *  LIST WILL HAVE
         *  42300,42200,42100
         */
        return strikePrices;
    }

    /**
     * This method monitors the PNL and do required actions based on PNL.
     */
    @Override
    public void monitorPositions(LiveFeedWSMessage liveFeedMessage) {
        if (Objects.isNull(liveFeedMessage)) {
            logger.info("**** monitorPositions > Empty Live feed message found ****");
            return;
        }
        Long tickerInstrumentToken = liveFeedMessage.getInstrumentToken();
        BigDecimal tickerLtp = liveFeedMessage.getLtp();
        //update today's high price of instrument
        setHighPriceOfInstrument(tickerInstrumentToken, tickerLtp);
        List<OpenOrderEntity> openOrders = orderService.getTodaysOrdersByInstrumentTokenAndStatus(liveFeedMessage.getInstrumentToken(), com.trade.algotrade.enums.OrderStatus.OPEN);
        logger.debug("*** monitorPositions > Monitoring positions Order Count := {} For Instrument := {} ***", openOrders.size(), liveFeedMessage.getInstrumentToken());
        Set<String> openOrderUsers = openOrders.stream().map(OpenOrderEntity::getUserId).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(openOrderUsers)) {
            logger.info("**** monitorPositions >> There is no open orders hence returning from monitorPositions flow ****");
            return;
        }
        // We are only processing Orders for the Open Order's User
        openOrderUsers.forEach(userId -> monitorUserOrders(liveFeedMessage, userId));
    }

    private void monitorUserOrders(LiveFeedWSMessage liveFeedMessage, String userId) {
        Long tickerInstrumentToken = liveFeedMessage.getInstrumentToken();
        UserResponse user = userService.getUserById(userId);
        List<OpenOrderEntity> allOpenSLOrdersByUsers = orderService.getAllOpenSLOrdersByUserId(userId);
        if (!CollectionUtils.isEmpty(allOpenSLOrdersByUsers)) {
            allOpenSLOrdersByUsers = allOpenSLOrdersByUsers.stream().filter(o -> o.getInstrumentToken().compareTo(tickerInstrumentToken) == 0).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(allOpenSLOrdersByUsers)) {
            logger.info("*** monitorPositions >> monitorUserOrders >> No Open SL Order found for User := {} Hence Returning from monitorUserOrders flow  ***", userId);
            return;
        }
        logger.debug("*** monitorPositions  >> monitorUserOrders >>SL Orders Found Count {}, User := {} and Instrument := {} ***", allOpenSLOrdersByUsers.size(), userId, tickerInstrumentToken);

        // Considering all Open SL Orders, We need to modify that to market order in certain cases
        // Here we have used For loop because we want to continue in certain cases
        for (OpenOrderEntity orderEntity : allOpenSLOrdersByUsers) {
//        Below code will run after every 1 minutes to check SL order status at kotak. If SL order is already
//        executed and not updated in algotrade database then update the order and trade immediatly with
//        order details from kotak
            Boolean flag = TradeUtils.isMonitorFLowStartedForSLOrder.get(orderEntity.getOrderId());
            if (Objects.isNull(flag) || Boolean.FALSE == flag) {
                TradeUtils.isMonitorFLowStartedForSLOrder.put(orderEntity.getOrderId(), Boolean.TRUE);
            }
            if (user.getIsRealTradingEnabled()) {
                LocalDateTime timerTime = TradeUtils.slCheckTimer.get(String.valueOf(orderEntity.getOrderId()));
                if (Objects.isNull(timerTime)) {
                    logger.info("1 MINUTE TIMER STARTED for  USER ID {}, ORDER ID {}", userId, orderEntity.getOrderId());
                    TradeUtils.slCheckTimer.put(String.valueOf(orderEntity.getOrderId()), DateUtils.getCurrentDateTimeIst());
                }
                if (Objects.nonNull(timerTime)) {
                    boolean isTimerExpired = timerTime.isBefore(DateUtils.getCurrentDateTimeIst().plusMinutes(-1)) || timerTime.isEqual(DateUtils.getCurrentDateTimeIst().plusMinutes(-1));
                    if (isTimerExpired) {
                        logger.info("*** 1 MINUTE TIMER ELAPSED :  Initiating every 1 minutes call to execute SL order at kotak for user {}", userId);
                        modifySLOrderToMarketOrder(liveFeedMessage, orderEntity, SLTriggerReasonEnum.SL_TIME_LIMIT_EXCEED.toString());
                        // Remove this slTimer for this specific order
                        TradeUtils.slCheckTimer.remove(String.valueOf(orderEntity.getOrderId()));
                    }
                }
            }
//            if (checkTrailEligible(liveFeedMessage, orderEntity, user)) {
//                continue;
//            }
//            if (checkMaxLossPerDayExceedWithCurrentOpenOrder(user, tickerInstrumentToken, tickerLtp, orderEntity)) {
//                continue;
//            }
//            isPriceDropped(tickerInstrumentToken, tickerLtp, orderEntity);
            if (CommonUtils.getOffMarketHoursTestFlag() && isSlTriggered(liveFeedMessage, orderEntity, user)) {
                continue;
            }
            isTargetReached(liveFeedMessage, orderEntity);
        }
        /**
         * Note Don't Delete this commented code
         *
         * This method will call when we have to monitor orders with No SL defined only target defined.
         *
         * targetSquareOff(tickerInstrumentToken, tickerLtp, orderEntity);
         * //This is last condition check so no need to return from loop
         *
         *
         */

    }

    private boolean checkTrailEligible(LiveFeedWSMessage liveFeedMessage, OpenOrderEntity openOrderEntity, UserResponse user) {
        logger.debug("[checkTrailEligible] called.. ");
        if (openOrderEntity.getOrderType() == OrderType.SL
                && ObjectUtils.isNotEmpty(liveFeedMessage)
                && liveFeedMessage.getInstrumentToken().compareTo(openOrderEntity.getInstrumentToken()) == 0) {
            Long originalOrderId = openOrderEntity.getOriginalOrderId();
            OpenOrderEntity currentOpenOrder = orderService.findOrderByOrderId(originalOrderId);
            if ((currentOpenOrder.getPrice() != null) || (BigDecimal.ZERO.compareTo(currentOpenOrder.getPrice()) != 0)) {
                BigDecimal currentOrderBuyValue = Objects.requireNonNull(currentOpenOrder.getPrice()).multiply(new BigDecimal(currentOpenOrder.getQuantity()));
                if (liveFeedMessage.getLtp() != null) {
                    BigDecimal currentValue = liveFeedMessage.getLtp().multiply(new BigDecimal(currentOpenOrder.getQuantity()));
                    if (currentValue.compareTo(currentOrderBuyValue) >= 0) {
                        return trailStopLoss(liveFeedMessage.getInstrumentToken(), liveFeedMessage.getLtp(), user, openOrderEntity);
                    }
                }
                logger.debug("[checkTrailEligible] Completed.. Without Trail");
                return false;
            }
        }
        logger.debug("[checkTrailEligible] Completed.. Without Trail");
        return false;
    }

    private boolean checkMaxLossPerDayExceedWithCurrentOpenOrder(UserResponse user, Long tickerInstrumentToken, BigDecimal ticketLtp, OpenOrderEntity openOrderEntity) {
        // Buy Value from Trade Table.
        logger.debug("[checkMaxLossPerDayExceedWithCurrentOpenOrder] called.. For User {} and Order ID {}", user.getUserId(), openOrderEntity.getOrderId());
        BigDecimal currentOpenTradeLoss = BigDecimal.ZERO;
        if (openOrderEntity.getOrderType() == OrderType.SL && tickerInstrumentToken.compareTo(openOrderEntity.getInstrumentToken()) == 0) {
            Long originalOrderId = openOrderEntity.getOriginalOrderId();
            OpenOrderEntity currentOpenOrder = orderService.findOrderByOrderId(originalOrderId);
            if (ObjectUtils.isNotEmpty(currentOpenOrder) && (currentOpenOrder.getPrice() != null && BigDecimal.ZERO.compareTo(currentOpenOrder.getPrice()) != 0)) {
                BigDecimal currentOrderBuyValue = currentOpenOrder.getPrice().multiply(new BigDecimal(currentOpenOrder.getQuantity()));
                if (ticketLtp != null) {
                    BigDecimal currentValue = ticketLtp.multiply(new BigDecimal(currentOpenOrder.getQuantity()));
                    currentOpenTradeLoss = currentValue.subtract(currentOrderBuyValue);
                }
            }
        }
        List<TradeEntity> allCompletedTrades = tradeService.getAllTodaysCompletedTrades(user.getUserId());
        if (!CollectionUtils.isEmpty(allCompletedTrades) && currentOpenTradeLoss.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal todaysRealisedPnl = allCompletedTrades.stream().map(TradeEntity::getRealisedPnl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayCurrentPNL = currentOpenTradeLoss.add(todaysRealisedPnl);
            Map<String, BigDecimal> dayLossLimit = user.getDayLossLimit();
            if (dayLossLimit != null) {
                BigDecimal segmentLossLimitPerDay = dayLossLimit.get(openOrderEntity.getSegment());
                if (dayCurrentPNL.compareTo(segmentLossLimitPerDay.negate()) <= 0) {
                    boolean isSquareOffDone = squareOffOrders(openOrderEntity);
                    logger.debug("[checkMaxLossPerDayExceedWithCurrentOpenOrder] Completed.. With Square Off DONE {}", isSquareOffDone);
                    return isSquareOffDone;
                }
            }
        }
        logger.debug("[checkMaxLossPerDayExceedWithCurrentOpenOrder] Completed without Square off.. ");
        return false;
    }

    private boolean isPriceDropped(Long tickerInstrumentToken, BigDecimal tickerLtp, OpenOrderEntity openOrderEntity) {
        logger.debug("Called for For Instrument Token {} LTP {} .. ", tickerInstrumentToken, tickerLtp);
        BigDecimal highPriceOfInstrument = TradeUtils.getPremiumHighValues().get(tickerInstrumentToken);
        //Apply reversal logic to square off order after minimun 1 SL trial
        if (highPriceOfInstrument != null
                && highPriceOfInstrument.compareTo(tickerLtp) > 0
                && openOrderEntity.getStrategyName().equals(Strategy.BIGCANDLE.toString())) {
            logger.debug("[isPriceDropped] Called for For Instrument Token {} LTP {} .. ", tickerInstrumentToken, tickerLtp);
            return isPriceDroppedToDefinedLimit(tickerInstrumentToken, tickerLtp, openOrderEntity);
        }
        logger.debug("[isPriceDropped] Completed for For Instrument Token {} LTP {} .. ", tickerInstrumentToken, tickerLtp);
        return false;
    }

    private boolean isSlTriggered(LiveFeedWSMessage liveFeedMessage, OpenOrderEntity openOrderEntity, UserResponse
            user) {
        logger.debug("[isSlTriggered] called.. ");
        if (openOrderEntity.getOrderType() == OrderType.SL) {
            SlDetails slDetails = openOrderEntity.getSlDetails();
            BigDecimal slPrice = slDetails.getSlPrice();
//            BigDecimal slBufferPrice = slDetails.getSlPrice();
//            if (TradeUtils.isBetweenRange(slPrice, slBufferPrice, liveFeedMessage.getLtp())) {
//            replacing between range logic with less than logic to handle sudden price drop
            if (slPrice.compareTo(liveFeedMessage.getLtp()) > 0) {
                boolean isModified = false;
                if (Boolean.valueOf(commonUtils.getConfigValue(ConfigConstants.IS_SL_TRIGGERED_FLAG)) == Boolean.TRUE && openOrderEntity.getOrderStatus().equals(com.trade.algotrade.enums.OrderStatus.OPEN)) {
                    synchronized (this) {
                        logger.info("SL order {} not executed at Kotak yet. Hence executing manually.", openOrderEntity.getOrderId());
                        modifySLOrderToMarketOrder(liveFeedMessage, openOrderEntity, SLTriggerReasonEnum.EXECUTE_SL.toString());
                    }
                }
                logger.info("SL order {} executed at Kotak.", openOrderEntity.getOrderId());
                return true;
            }
        }
        logger.debug("[isSlTriggered] Completed with FALSE.. ");
        return false;
    }

    private void isTargetReached(LiveFeedWSMessage liveFeedMessage, OpenOrderEntity openOrderEntity) {
        logger.debug("[isTargetReached] called.. ");
        BigDecimal slPrice = openOrderEntity.getSlDetails().getTargetPrice();
        if (slPrice.compareTo(liveFeedMessage.getLtp()) < 0) {
            modifySLOrderToMarketOrder(liveFeedMessage, openOrderEntity, SLTriggerReasonEnum.TARGET_REACH.toString());
            logger.info("Target is reached, modifying SL order {} to market.", openOrderEntity.getOrderId());
        }
    }

    private void modifySLOrderToMarketOrder(LiveFeedWSMessage liveFeedMessage, OpenOrderEntity openOrderEntity, String reason) {
        Boolean slOrderFallbackFlag = TradeUtils.slOrderFallBackCallFlag.get(openOrderEntity.getOrderId());
        logger.info("Modifying Order for User ID {} and Reason {}, slOrderFallbackFlag {}", openOrderEntity.getUserId(), reason, slOrderFallbackFlag);

        if (Objects.isNull(slOrderFallbackFlag)) {
            TradeUtils.slOrderFallBackCallFlag.put(openOrderEntity.getOrderId(), Boolean.FALSE);
            boolean isModified = false;
            if (openOrderEntity.getOrderStatus().equals(com.trade.algotrade.enums.OrderStatus.OPEN)) {
                synchronized (this) {
                    try {
                        openOrderEntity.setPrice(liveFeedMessage.getLtp());
                        isModified = orderService.modifySlOrderToMarketOrder(openOrderEntity, reason);
                    } catch (Exception e) {
                        logger.error("Failed to modify order {}", openOrderEntity.getOrderId());
                    }
                }
            }
            if (isModified) {
                logger.info("Async call to update trade and order after execution of SL.");
                TradeUtils.slOrderFallBackCallFlag.put(openOrderEntity.getOrderId(), Boolean.FALSE);
//                runUpdateSLOrderInTreadThread(openOrderEntity);
                processOrderFeed();
                // trigger pending order feed should process.
            } else {
                TradeUtils.slOrderFallBackCallFlag.remove(openOrderEntity.getOrderId());
                logger.info("There is no Order {} to Modify for User {}, Reason Was {}", openOrderEntity.getOrderId(), reason);
            }
        }
    }

    //If SL order executed at kotak and we didn't receive the notification then after 3 sec update trade table.
    private boolean updateSLOrderAndTradeStatusFallBack(OpenOrderEntity openOrderEntity) {
        OpenOrderEntity orderEntity = orderService.findOrderByOrderId(openOrderEntity.getOrderId());
        if (orderEntity.getOrderCategory().equals(OrderCategoryEnum.SQAREOFF) && com.trade.algotrade.enums.OrderStatus.OPEN.equals(orderEntity.getOrderStatus())) {
            List<OrderResponse> allOrders = orderService.getAllOrders(orderEntity.getUserId());
            if (CommonUtils.getOffMarketHoursTestFlag()) {
                allOrders.get(0).setOrderId(orderEntity.getOrderId());
                allOrders.get(0).setInstrumentToken(orderEntity.getInstrumentToken());
            }
            Optional<OrderResponse> optionalKotakOrder = allOrders.stream().filter(order -> order.getOrderId().equals(orderEntity.getOrderId())).findFirst();
            if (optionalKotakOrder.isPresent()) {
                OrderResponse orderResponse = optionalKotakOrder.get();
                OrderFeedWSMessage message = OrderFeedWSMessage.builder().orderId(String.valueOf(orderEntity.getOrderId())).userId(orderEntity.getUserId()).price(String.valueOf(orderResponse.getPrice())).quantity(String.valueOf(orderResponse.getQuantity())).status(com.trade.algotrade.client.kotak.enums.OrderStatus.FIL.toString()).build();
                handleSquareOffOrdersAndUpdatePnl(message, orderEntity);
            }
        }
        List<TradeEntity> openTradesFoundForToday = tradeService.getOpenTradesFoundForToday();
        if (CollectionUtils.isEmpty(openTradesFoundForToday)) {
            return true;
        }
        return false;
    }

    private boolean targetSquareOff(Long tickerInstrumentToken, BigDecimal ticketLtp, OpenOrderEntity order) {
        if (order.getTargetDetails() == null || ticketLtp == null) {
            return false;
        }
        boolean targetSquareoff = false;
        BigDecimal targetPrice = order.getTargetDetails().getTargetPrice();
        if (targetPrice != null) {
            if (ticketLtp.compareTo(targetPrice) >= 0) {
                // squareOffOrders(tickerInstrumentToken, Collections.singletonList(order));
                targetSquareoff = true;
            }
            return targetSquareoff;
        }
        BigDecimal targetPercent = order.getTargetDetails().getTargetPercent();
        if (targetPercent != null) {
            BigDecimal targetPercentPrice = TradeUtils.getXPercentOfY(targetPercent, order.getPrice());
            if (targetPercentPrice != null && ticketLtp.compareTo(targetPercentPrice) >= 0) {
                // squareOffOrders(tickerInstrumentToken, Collections.singletonList(order));
                targetSquareoff = true;
            }
        }

        if (targetSquareoff && order.getIsSlOrderPlaced()) {
            // this means order already having one SL placed that need to modify to market order.
            OpenOrderEntity squareOffOrder = orderService.getOpenSquareOffOrderByInstrumentToken(tickerInstrumentToken);
            squareOffOrder.setPrice(null);
        }
        return targetSquareoff;
    }


    private boolean isPriceDroppedToDefinedLimit(Long tickerInstrumentToken, BigDecimal ticketLtp, OpenOrderEntity
            order) {
        StrategyResponse strategy = strategyService.getStrategy(order.getStrategyName());
        int reversePoints = Integer.parseInt(strategy.getExitCondition().getConditions().get(Constants.BIG_CANDLE_REVERSE_POINTS));
        BigDecimal highPriceOfInstrument = TradeUtils.getPremiumHighValues().get(tickerInstrumentToken);
        if (highPriceOfInstrument != null
                && highPriceOfInstrument.subtract(ticketLtp).compareTo(new BigDecimal(reversePoints)) >= 0
                && Boolean.FALSE == TradeUtils.slOrdersExecutionFlags.get(order.getOriginalOrderId())
                && order.getOrderStatus().equals(com.trade.algotrade.enums.OrderStatus.OPEN)) {
            logger.info("**** checkPriceDropExitCondition > Squaring off call order against the Instrument := {} at Price := {} as market reversed by := {} points.", tickerInstrumentToken, ticketLtp, reversePoints);
            if (!modifySlOrLimitOrderToMarketOrder(order, SLTriggerReasonEnum.REVERSAL_SL)) {
                logger.info("Async call to update trade and order after execution of SL.");
//                runUpdateSLOrderInTreadThread(order);
            }
        }
        return false;
    }

    private boolean modifySlOrLimitOrderToMarketOrder(OpenOrderEntity slOrderEntity, SLTriggerReasonEnum slTriggerReasonEnum) {
        boolean isOrderMarketOrderPlaced = orderService.modifySlOrderToMarketOrder(slOrderEntity, slTriggerReasonEnum.toString());
        TradeUtils.slOrdersExecutionFlags.put(slOrderEntity.getOriginalOrderId(), Boolean.TRUE);
        if (isOrderMarketOrderPlaced) {
            logger.info("Async call to update trade and order after execution of SL.");
//            runUpdateSLOrderInTreadThread(slOrderEntity);
        }
        return isOrderMarketOrderPlaced;
    }

    /**
     * This method will trial stop loss to buy price when order profit is greater
     * than buy price + spread
     *
     * @param ltpPrice
     * @param user
     * @param instrumentToken
     */
    private boolean trailStopLoss(Long instrumentToken, BigDecimal ltpPrice, UserResponse user, OpenOrderEntity
            order) {
        logger.debug("*** Inside trail stop-loss Instrument Token := {}, At Price := {} for User := {} ***", instrumentToken, ltpPrice, user.getUserId());
        switch (order.getStrategyName()) {
            case Constants.BIG_CANDLE_STRATEGY:
                //NOTE: Next price is addition of Original order price + SL spread
                SlDetails slDetails = order.getSlDetails();
                BigDecimal nextTargetPrice = order.getSlDetails().getSlNextTargetPrice();
                if (ltpPrice.compareTo(nextTargetPrice) > 0) {
                    BigDecimal orderPrice;
                    SlDetails details = new SlDetails();
                    if (order.getModificationCount() == 0) {
                        orderPrice = order.getPrice().add(order.getSlDetails().getSlPoints()).add(BigDecimal.ONE);
                        details.setSlNextTargetPrice(slDetails.getSlNextTargetPrice().add(slDetails.getSpread()));
                    } else {
                        BigDecimal nextTargetPriceDifference = ltpPrice.subtract(nextTargetPrice).setScale(0, RoundingMode.DOWN); // Last Price 320  and Next TargetPrice :291 =29
                        BigDecimal spread = slDetails.getSpread(); // Spread 5
                        BigDecimal multiples = nextTargetPriceDifference.divide(spread).setScale(0, RoundingMode.DOWN);
                        ; // Multiplies 5 : 29/5= 5
                        if (multiples.compareTo(BigDecimal.ONE) >= 0) {
                            // This logic is to handle Jump increase in LTP
                            BigDecimal increaseByGap = spread.multiply(multiples);
                            orderPrice = order.getPrice().add(increaseByGap);
                            details.setSlNextTargetPrice(slDetails.getSlNextTargetPrice().add(increaseByGap));
                            logger.debug("Multiples {} and increase by gap {}", multiples, increaseByGap);
                        } else {
                            orderPrice = order.getPrice().add(spread);
                            details.setSlNextTargetPrice(slDetails.getSlNextTargetPrice().add(spread));
                        }
                    }
                    details.setSlPrice(orderPrice.subtract(slDetails.getSlBufferPoints()));
                    details.setTriggerPrice(orderPrice);
                    details.setSpread(slDetails.getSpread());
                    details.setTrailingStartPrice(slDetails.getTrailingStartPrice());
                    details.setSlBufferPoints(slDetails.getSlBufferPoints());

                    logger.debug("**** Trailing stop loss from price {} to {} for Instrument Token := {} and Order := {}", order.getPrice(), orderPrice, instrumentToken, order.getOrderId());
                    OrderRequest orderRequest = OrderRequest.builder()
                            .instrumentToken(order.getInstrumentToken())
                            .transactionType(order.getTransactionType())
                            .quantity(order.getQuantity())
                            .optionType(order.getOptionType())
                            .orderType(order.getOrderType())
                            .price(orderPrice)
                            .userId(user.getUserId())
                            .product(com.trade.algotrade.client.angelone.enums.ProductType.INTRADAY)
                            .variety(VarietyEnum.NORMAL)
                            .strategyName(order.getStrategyName())
                            .slDetails(details)
                            .orderCategory(order.getOrderCategory())
                            .orderId(order.getOrderId())
                            .build();
                    List<OrderResponse> orderResponseList = orderService.modifyOrder(order.getOrderId(), orderRequest, null, false);
                    if (!orderResponseList.isEmpty()) {
                        logger.info("Modifying SL order for User ID := {}, price from {} to {}", user.getUserId(), order.getPrice(), orderPrice);
                        return true;
                    }
                }
                break;
            case Constants.STRATEGY_EXPIRY:
                break;

            default:
        }
        return false;
    }

    /**
     * This method will square off all the BUY orders of a user, if the total orders
     * for same instrument in same day has loss greater than day loss limit.
     */
    private boolean squareOffOrders(OpenOrderEntity orderEntity) {
        if (!TradeUtils.slOrdersExecutionFlags.get(orderEntity.getOriginalOrderId())) {
            logger.info("**** checkPriceDropExitCondition > Squaring off call order against the Instrument := {} at market Price. As day Max loss condition matched with current trade", orderEntity.getInstrumentToken());
            orderService.modifySlOrderToMarketOrder(orderEntity, SLTriggerReasonEnum.DAY_LOSS_EXCEED.toString());
            TradeUtils.slOrdersExecutionFlags.put(orderEntity.getOriginalOrderId(), Boolean.TRUE);
            return true;
        }
        return false;
    }

    public List<OrderResponse> processBigCandleStrategyOrder(String userIds, AngelOneInstrumentMasterEntity
            kotakInstrumentMasterEntity, OptionType optionType, StrategyResponse strategy) {
        logger.debug("processBigCandleStrategy for all active users Strike Price : = {}  and for Option Type := {}", kotakInstrumentMasterEntity.getStrike(), optionType);
        OrderRequest orderRequest = getOrderRequest(userIds, kotakInstrumentMasterEntity, optionType);
        logger.info("processBigCandleStrategy > Placing order for all active users Strategy := {},Strike Price  : = {} and Option Type := {}", strategy.getStrategyName(), kotakInstrumentMasterEntity.getStrike(), optionType);
        return prepareStrategyOrder(orderRequest, strategy);
    }

    private void setHighPriceOfInstrument(Long instrumentToken, BigDecimal ltp) {
        BigDecimal oldHighPrice = TradeUtils.getPremiumHighValues().get(instrumentToken);
        if (Objects.isNull(oldHighPrice)) {
            TradeUtils.setPremiumHighValues(instrumentToken, ltp);
        } else if (oldHighPrice.compareTo(ltp) < 0) {
            TradeUtils.setPremiumHighValues(instrumentToken, ltp);
        }
    }

    private BigDecimal findLowPriceOfInstrument(Long instrumentToken, BigDecimal ltp) {
        BigDecimal price;
        BigDecimal oldLowPrice = TradeUtils.getPremiumLowValues().get(instrumentToken);
        if (Objects.isNull(oldLowPrice)) {
            TradeUtils.setPremiumLowValues(instrumentToken, ltp);
            price = ltp;
        } else {
            price = oldLowPrice;
            if (price.compareTo(ltp) > 0) {
                TradeUtils.setPremiumLowValues(instrumentToken, ltp);
                price = ltp;
            }
        }
        return price;
    }

    private List<OrderResponse> prepareStrategyOrder(OrderRequest orderRequest, StrategyResponse strategyResponse) {
        logger.debug("Preparing {} Strategy order started.", strategyResponse.getStrategyName());
        Map<String, String> conditions = strategyResponse.getExitCondition().getConditions();
        String defaultSlSpread = conditions.get(Constants.DEFAULT_SL_SPREAD);
        SlDetails slDetails = orderRequest.getSlDetails();
        // IF SL details are set from somewhere else same SL details should go
        if (slDetails == null) {
            slDetails = new SlDetails();
            if (!StringUtils.isEmpty(defaultSlSpread)) {
                slDetails.setSpread(new BigDecimal(defaultSlSpread));
            }
            // Override Default Figure with digit
            String defaultSlSpreadPercent = conditions.get(Constants.DEFAULT_SL_SPREAD_PERCENT);
            if (StringUtils.isNotEmpty(defaultSlSpreadPercent)) {
                slDetails.setSlPercent(new BigDecimal(defaultSlSpreadPercent));
            }
            String stopLossPoints = conditions.get(Constants.DEFAULT_SL_POINT);
            if (StringUtils.isEmpty(stopLossPoints)) {
                throw new AlgotradeException(ErrorCodeConstants.NOT_STOP_LOSS_POINTS_FOUND);
            }
            slDetails.setSlPoints(new BigDecimal(stopLossPoints));

            String slBufferPoints = conditions.get(Constants.DEFAULT_SL_BUFFER_POINT);
            if (StringUtils.isNotEmpty(slBufferPoints)) {
                slDetails.setSlBufferPoints(new BigDecimal(slBufferPoints));
            }
        }
        slDetails.setTrailSl(true);
        orderRequest.setSlDetails(slDetails);
        orderRequest.setStrategyName(strategyResponse.getStrategyName());
        orderRequest.setProduct(com.trade.algotrade.client.angelone.enums.ProductType.INTRADAY);
        orderRequest.setVariety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL);
        logger.info("Preparing {} Strategy order Completed", strategyResponse.getStrategyName());
        return orderService.prepareOrderAndCreateOrder(orderRequest);
    }

    public void addSLAndMarginForPlacedOrder(OrderFeedWSMessage orderFeedWSMessage) {

        logger.info("User ID := {} called for  Order ID :={}, Message Source :={}, Status := {}", orderFeedWSMessage.getUserId(), orderFeedWSMessage.getOrderId(), orderFeedWSMessage.getMessageSource(), orderFeedWSMessage.getStatus());
        OpenOrderEntity openOrderEntity = getOrderEntityWithRetry(orderFeedWSMessage);
        if (openOrderEntity == null) return;
        if (validateOrderStatus(orderFeedWSMessage, openOrderEntity)) return;
        if (openOrderEntity.getOrderCategory() == OrderCategoryEnum.NEW && !openOrderEntity.getIsSlOrderPlaced()) {
            handleNewOrdersSlAndMargin(orderFeedWSMessage, openOrderEntity);
            TradeUtils.orderFeedPushQuantityList.remove(openOrderEntity.getOrderId());
        } else
            handleSquareOffOrdersAndUpdatePnl(orderFeedWSMessage, openOrderEntity);
    }

    private OpenOrderEntity getOrderEntityWithRetry(OrderFeedWSMessage orderFeedWSMessage) {
        OpenOrderEntity openOrderEntity = null;
        logger.debug("User Id {}, order Id {} fetching order from db. Feed : {}", orderFeedWSMessage.getUserId(), orderFeedWSMessage.getOrderId(), orderFeedWSMessage);
        for (int orderRetryAttempt = 0; orderRetryAttempt < Constants.MAX_RETRY_PLACE_ORDER; orderRetryAttempt++) {
            try {
                openOrderEntity = orderService.getOpenOrderByOrderId(Long.valueOf(orderFeedWSMessage.getOrderId()));
            } catch (AlgoValidationException e) {
                logger.info("Get order for order feed exception {}", e.getMessage());
            }
            if (Objects.nonNull(openOrderEntity)) {
                logger.debug("User Id {}, order Id {} found in db.", orderFeedWSMessage.getUserId(), orderFeedWSMessage.getOrderId());
                break;
            } else {
                try {
                    logger.info("Order not found in DB hence retrying  attempt {}, User ID {}, order  ID {}", orderRetryAttempt, orderFeedWSMessage.getUserId(), orderFeedWSMessage.getOrderId());
                    Thread.sleep(500);// Before saving the order in DB getting order feed.
                } catch (InterruptedException e) {
                    logger.error("Failed in get order from DB retry for user {}, order {}", orderFeedWSMessage.getUserId(), orderFeedWSMessage.getOrderId());
                }
            }
        }

        if (Objects.isNull(openOrderEntity)) {
            logger.info("After Retrying {} times, Still We didn't find Order {} in DB", Constants.MAX_RETRY_PLACE_ORDER, orderFeedWSMessage.getOrderId());
            return null;
        }
        return openOrderEntity;
    }

    private boolean validateOrderStatus(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity openOrderEntity) {
        if (com.trade.algotrade.enums.OrderStatus.EXECUTED == openOrderEntity.getOrderStatus()) {
            // As there is no change in order status
            logger.info("DB status is already executed, Hence returning from addSLAndMarginForPlacedOrder flow");
            return true;
        }
        com.trade.algotrade.client.angelone.enums.OrderStatus orderStatus = com.trade.algotrade.client.angelone.enums.OrderStatus.fromValue(orderFeedWSMessage.getStatus());
        if (OrderStatus.CAN == orderStatus) {
            openOrderEntity.setOrderStatus(com.trade.algotrade.enums.OrderStatus.CANCELLED);
            orderService.saveOpenOrders(openOrderEntity);
            return true;
        }
        if (OrderStatus.RJT == orderStatus) {
            logger.info("Order {} failed for User {} Message Source := {},Status is := {}", orderFeedWSMessage.getOrderId(), openOrderEntity.getUserId(), orderFeedWSMessage.getMessageSource(), orderFeedWSMessage.getStatus());
            updateTradeIfOrderRejected(openOrderEntity);
            return true;
        }
        return false;
    }

    //This is fall back check for order target verification.
    // In case monitor flow fails this thread will execute order once target is reached.
    private void runTargetExecuteVerificationThread(Long slOrderId) {
        logger.info("Inside monitor fallback method");
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Timer timer = new Timer();
        String tradeMonitorIntervalInMinutes = commonUtils.getConfigValue(ConfigConstants.FNO_TARGET_MONITORING_THREAD_DURATION);
        if (tradeMonitorIntervalInMinutes == null) {
            tradeMonitorIntervalInMinutes = "1"; // Default Interval is 1 min
        }
        long interval = Long.parseLong(tradeMonitorIntervalInMinutes) * 1000 * 60;
        long delay = 3000;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    OpenOrderEntity slOrderEntity = orderService.getOpenOrderByOrderId(slOrderId);
                    if (slOrderEntity.getOrderStatus().equals(com.trade.algotrade.enums.OrderStatus.OPEN)) {
                        Long instrumentToken = slOrderEntity.getInstrumentToken();
                        logger.info("Getting LTP ORDER ID {} Instrument {} Target Completed Timer Thread", slOrderId, instrumentToken);
                        QouteResponse ltp = angelOneClient.getQuote(ExchangeType.NFO, List.of(String.valueOf(instrumentToken)));
                        if (ltp != null && ltp.getData() != null && !CollectionUtils.isEmpty(ltp.getData().getFetched())) {
                            BigDecimal lastPrice = ltp.getData().getFetched().stream().findFirst().get().getLtp();
                            if (slOrderEntity.getSlDetails().getTargetPrice().compareTo(lastPrice) < 0) {
                                future.complete(modifySlOrLimitOrderToMarketOrder(slOrderEntity, SLTriggerReasonEnum.TARGET_REACH));
                            }
                        }
                    }
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            }
        }, delay, interval);
        future.thenAccept(isThreadWorkCompleted -> {
            if (isThreadWorkCompleted) {
                logger.info("runTargetExecuteVerificationThread OrderEntity : NO OPEN TRADE FOUND, HENCE CANCELLING TRADE MONITORING TASK");
                timer.cancel();
            }
        }).join();
    }

//    private boolean validateOrderFeedAction(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity openOrderEntity) {
//        com.trade.algotrade.client.angelone.enums.OrderStatus orderStatus = com.trade.algotrade.client.angelone.enums.OrderStatus.fromValue(orderFeedWSMessage.getStatus());
//        switch (orderStatus) {
//            case OPN:
//                return false;
//            case FIL:
//                logger.info("{} order executed for User {} Message Source := {},Status is := {},Order ID:= {}", orderFeedWSMessage.getBuySell(), openOrderEntity.getUserId(), orderFeedWSMessage.getMessageSource(), orderFeedWSMessage.getStatus(), orderFeedWSMessage.getOrderId());
//                return false;
//            case CAN:
//                openOrderEntity.setOrderStatus(com.trade.algotrade.enums.OrderStatus.CANCELLED);
//                orderService.saveOpenOrders(openOrderEntity);
//                //TODO If order is cancelled and its New Order then We need to cancel Trade if Trade having only one order ]
//                // If its SL order modification failed then Need to Retry for SL modification
//                logger.info("[validateOrderFeedAction] Cancelled Order for ID:= {}", orderFeedWSMessage.getOrderId());
//                return true;
//            //TODO: Need to update this code for Rejected status
//            case RJT:
//                logger.info("Order {} failed for User {} Message Source := {},Status is := {}", orderFeedWSMessage.getOrderId(), openOrderEntity.getUserId(), orderFeedWSMessage.getMessageSource(), orderFeedWSMessage.getStatus());
//                updateTradeIfOrderRejected(openOrderEntity);
//                return true;
//            case TRP:
//                return true;
//            default:
//                // no need to execute next flow if order feed status is other than FIL and TRAD.. hence return true.
//                logger.info("Order feed status default {}", orderStatus);
//                return true;
//
//        }
//    }

    @Override
    public boolean ignoreActionOnOrderFeedStatues(OrderFeedWSMessage orderFeedWSMessage) {
        String status = orderFeedWSMessage.getStatus();
        if (StringUtils.isEmpty(status)) {
            return true;
        }
        OrderStatus orderStatus = OrderStatus.fromValue(status);
        switch (orderStatus) {
            case FIL:
            case TRP:
            case CAN:
            case RJT:
                return false;
            default:
                return true;
        }
    }

    private void handleNewOrdersSlAndMargin(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity openOrderEntity) {
        logger.info("[handleNewOrdersSlAndMargin] placing SL for User := {} ORDER ID := {} ", openOrderEntity.getUserId(), openOrderEntity.getOrderId());
//        if (orderFeedWSMessage.getPrice() == null || BigDecimal.ZERO.compareTo(new BigDecimal(orderFeedWSMessage.getPrice())) == 0) {
//            // In case of 0 value no need to update order status
//            logger.info("Buy order feed price is {}, hence returning", orderFeedWSMessage.getPrice());
//            return;
//        }
//        if (BooleanUtils.isTrue(openOrderEntity.getIsSlOrderPlaced())) {
//            logger.info("SL Flag is := {}, ORDER ID := {}", openOrderEntity.getIsSlOrderPlaced(), openOrderEntity.getOrderId());
//            return;
//        }
        // boolean slOrderPlaced = prepareAndPlaceSlOrder(orderFeedWSMessage, openOrderEntity);
        OrderRequest orderRequest = TradeUtils.scalpOpenOrders.get(openOrderEntity.getInstrumentToken());
        if (Objects.isNull(orderRequest)) {
            return;
        }

        UserResponse userResponse = userService.getUserById(openOrderEntity.getUserId());
        Boolean slOrderPlaced = Boolean.FALSE;

        OpenOrderEntity slOpenOrderEntity = orderService.getSLOrderByOriginalOrderId(openOrderEntity.getOrderId());
        if (Objects.isNull(slOpenOrderEntity)) {
            try {
                prepareSLPrices(orderFeedWSMessage, openOrderEntity, orderRequest);
                prepareSLOrder(orderFeedWSMessage, openOrderEntity, orderRequest);

                CompletableFuture<Void> priceUpdateThread = CompletableFuture.runAsync(() -> {
                    updateOrderStatusAndPriceAsync(openOrderEntity, orderFeedWSMessage);
                });

                OpenOrderEntity slOrderEntity = orderService.placeDirectSLOrder(orderRequest, userResponse);
                priceUpdateThread.join();
                if (Objects.isNull(slOrderEntity)) {
                    logger.info("SL is not placed for ORDER ID :={}", openOrderEntity.getOrderId());
                } else {
                    slOrderPlaced = Boolean.TRUE;
                    updateSLFlagforOrder(openOrderEntity, slOrderPlaced);
                    TradeUtils.slOrdersExecutionFlags.put(Long.valueOf(orderFeedWSMessage.getOrderId()), Boolean.FALSE);
                    logger.info("SL is placed for ORDER ID :={}", openOrderEntity.getOrderId());
                    TradeUtils.slOrderFallBackCallFlag.put(openOrderEntity.getOrderId(), Boolean.TRUE);
                    TradeUtils.isMonitorFLowStartedForSLOrder.put(slOrderEntity.getOrderId(), Boolean.FALSE);
//                    checkMonitorFlowStarted(slOrderEntity);
                    websocketUtils.addAndSubscribeInstrument(orderRequest.getInstrumentToken());
                    if (CommonUtils.getOffMarketHoursTestFlag()) {
                        websocketUtils.subscribeKotakOrderWebsocketMock(slOrderEntity.getOrderId(), slOrderEntity.getUserId(), String.valueOf(slOrderEntity.getPrice()), "TRP");
                    }
                }
            } catch (KotakTradeApiException kotakTradeApiException) {
                logger.error("DIRECT SL ORDER FAILED AT Broker FOR ORDER ID := {},EXECUTION PRICE := {} UPDATED", openOrderEntity.getOrderId(), openOrderEntity.getPrice());
            } catch (Exception e) {
                logger.error("DIRECT SL ORDER FAILED FOR ORDER ID := {},EXECUTION PRICE := {} UPDATED", openOrderEntity.getOrderId(), openOrderEntity.getPrice());
            }

            logger.info("[handleNewOrdersSlAndMargin] updating margin for User := {} ORDER ID := {} ", openOrderEntity.getUserId(), openOrderEntity.getOrderId());

            if (slOrderPlaced) {
                processOrderFeed();
                TradeUtils.scalpOpenOrders.remove(openOrderEntity.getInstrumentToken());
//                runTargetExecuteVerificationThread(slOrderEntity.getOrderId());
                CompletableFuture<Void> notificationAsyncThread = CompletableFuture.runAsync(() -> {
                    orderFeedNotification(orderFeedWSMessage, openOrderEntity);
                });
                notificationAsyncThread.join();
            }
        }
    }

    private void orderFeedNotification(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity openOrderEntity) {
        UserEntity userEntity = userService.getUserByUserId(openOrderEntity.getUserId());
        String message = MessageFormat.format("ORDER FEED Received  : order id {0} executed at broker with price \"{1}\" and quantity \"{2}\"", openOrderEntity.getOrderId(), orderFeedWSMessage.getPrice(), orderFeedWSMessage.getQuantity());
        notificationService.sendTelegramNotification(userEntity.getTelegramChatId(), message);
    }

    private void prepareSLOrder(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity openOrderEntity, OrderRequest orderRequest) {
        logger.info("Preparing SL Request SL ORDER ID :={}", openOrderEntity.getOrderId());
        orderRequest.setOrderType(OrderType.SL);
        orderRequest.setQuantity(openOrderEntity.getQuantity());
        orderRequest.setOriginalOrderId(Long.valueOf(orderFeedWSMessage.getOrderId()));
        orderRequest.setOrderCategory(OrderCategoryEnum.SQAREOFF);
        orderRequest.setVariety(VarietyEnum.STOPLOSS);
        orderRequest.setTransactionType(inverseTransactionType(openOrderEntity));
        logger.info("Prepare SL Request ORDER ID :={}", openOrderEntity.getOrderId());
    }

    private void prepareSLPrices(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity openOrderEntity, OrderRequest orderRequest) {
        SlDetails slDetails = openOrderEntity.getSlDetails();
        if (slDetails != null) {
            logger.info("SL is added to order hence preparing SL for ORDER ID :={}", openOrderEntity.getOrderId());
            BigDecimal buyPrice = new BigDecimal(orderFeedWSMessage.getPrice());
            BigDecimal defaultSlPoint = slDetails.getSlPoints();

            BigDecimal slPrice = BigDecimal.ZERO;
            if (defaultSlPoint != null) {
                slPrice = buyPrice.subtract(getValidSLPoints(buyPrice, defaultSlPoint));
            }
            // If slPercent is not null means we need to override it with percentage
            BigDecimal slPercent = slDetails.getSlPercent();
            if (slPercent != null && slPercent.compareTo(BigDecimal.ZERO) != 0) {
                slPrice = buyPrice.subtract(getValidSLPoints(buyPrice, TradeUtils.getXPercentOfY(slPercent, buyPrice)));
            }
            // Still sl price is null means SL details are not defined for this order use min percent on buy price allowed by broker.
            if (slPrice.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal validSLPoints = getValidSLPoints(buyPrice, null);
                slPrice = buyPrice.subtract(validSLPoints);
                slDetails.setSlPoints(validSLPoints);
            }


            BigDecimal targetPrice = buyPrice.add(new BigDecimal(commonUtils.getConfigValue(ConfigConstants.SCALPING_TARGET_POINTS)));
            slDetails.setTargetPrice(targetPrice.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.TEN : targetPrice);
            BigDecimal targetPercent = slDetails.getTargetPercent();
            if (targetPercent != null) {
                slDetails.setTargetPrice((buyPrice.add(TradeUtils.getXPercentOfY(targetPercent, buyPrice))));
            }

            slDetails.setTriggerPrice(slPrice);
            slDetails.setTrailSl(slDetails.isTrailSl());
            slDetails.setTrailingStartPrice(buyPrice.add(slDetails.getSpread()));
            slDetails.setSlNextTargetPrice(buyPrice.add(slDetails.getSpread()));
            slDetails.setSpread(slDetails.getSpread());
            orderRequest.setPrice(slPrice);
            slDetails.setSlPrice(slPrice.subtract(slDetails.getSlBufferPoints()));
            orderRequest.setSlDetails(slDetails);
            logger.info("SL is prepared for ORDER ID :={}", openOrderEntity.getOrderId());
        }
    }

    private void updateOrderStatusAndPriceAsync(OpenOrderEntity openOrderEntity, OrderFeedWSMessage orderFeedWSMessage) {
        logger.info("Updating order Status Async for Order ID {}", openOrderEntity.getOrderId());
        if (openOrderEntity.getOrderType() == OrderType.MARKET) {
            openOrderEntity.setPrice(new BigDecimal(orderFeedWSMessage.getPrice()));
            openOrderEntity.setOrderStatus(com.trade.algotrade.enums.OrderStatus.EXECUTED);
            openOrderEntity.getSlDetails().setTrailingStartPrice(openOrderEntity.getPrice().add(openOrderEntity.getSlDetails().getSpread()));
            logger.info("ORDER ID := {},EXECUTION PRICE := {} UPDATED", openOrderEntity.getOrderId(), openOrderEntity.getPrice());
            orderService.saveOpenOrders(openOrderEntity);
        }
    }

    private void updateSLFlagforOrder(OpenOrderEntity openOrderEntity, Boolean slOrderPlaced) {
        openOrderEntity.setIsSlOrderPlaced(BooleanUtils.isTrue(slOrderPlaced));
        logger.info("SL FLAG UPDATED for ORDER ID {} SL FALG {}", openOrderEntity.getOrderId(), slOrderPlaced);
        orderService.saveOpenOrders(openOrderEntity);
    }

    private CompletableFuture<Void> updateMarginCompletable(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity
            openOrderEntity) {
        return CompletableFuture.runAsync(() -> {
            BigDecimal updatedMargin = marginService.getMargin(orderFeedWSMessage.getUserId()).subtract(openOrderEntity.getPrice().multiply(new BigDecimal(Integer.parseInt(orderFeedWSMessage.getQuantity()))));
            marginService.updateMarginInDB(orderFeedWSMessage.getUserId(), updatedMargin);
        });
    }

    private void handleSquareOffOrdersAndUpdatePnl(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity
            openOrderEntity) {
        logger.info("Handling SL order {} feed to complete trade", openOrderEntity.getOrderId());
        // This case SquareOff case, Original order id is not null means case is of Target or SL order
        if (openOrderEntity.getOriginalOrderId() != null) {
            OpenOrderEntity originalOrder = orderService.getOpenOrderByOrderId(openOrderEntity.getOriginalOrderId());
            if (originalOrder != null && (orderFeedWSMessage.getStatus().equalsIgnoreCase(OrderStatusWebsocket.FIL.toString())) && openOrderEntity.getOrderStatus() == com.trade.algotrade.enums.OrderStatus.OPEN) {
                Optional<TradeEntity> currentTrade = tradeService.getTradeForOrder(openOrderEntity.getId());
                BigDecimal buyPrice = originalOrder.getPrice();
                String sellPrice = orderFeedWSMessage.getPrice();
                BigDecimal pnlPoints = new BigDecimal(sellPrice).subtract(buyPrice);
                CompletableFuture<Integer> userStrategyUpdateThread = CompletableFuture.supplyAsync(() -> updateUserStrategyDetails(openOrderEntity, pnlPoints, buyPrice, sellPrice));
                openOrderEntity.setPrice(new BigDecimal(orderFeedWSMessage.getPrice()));
                if (currentTrade.isEmpty()) {
                    logger.info("No trade found with Trade ID := {}", openOrderEntity.getTradeId());
                    return;
                }

                BigDecimal buyTotal = originalOrder.getPrice().multiply(new BigDecimal(originalOrder.getQuantity()));
                BigDecimal squareOffPrice = new BigDecimal(orderFeedWSMessage.getPrice()).multiply(new BigDecimal(openOrderEntity.getQuantity()));
                BigDecimal realisedPnl = squareOffPrice.subtract(buyTotal);
                CompletableFuture<TradeEntity> tradeEntityCompletableFuture = CompletableFuture.supplyAsync(() -> updateTradeDetails(orderFeedWSMessage, openOrderEntity, currentTrade, realisedPnl));
                Integer recoveryStrategyFailureCount = userStrategyUpdateThread.join();
                logger.info("Joined Strategy Failure Count Thread : Count := {}", recoveryStrategyFailureCount);
                TradeEntity tradeEntity = tradeEntityCompletableFuture.join();
                logger.info("Joined Trade Update Thread : Trade Entity New  := {}", tradeEntity);
                if (tradeEntity == null) return;
                if (recoveryStrategyFailureCount > 0) {
                    tradeEntity.setRecoveryStrategyFailureCount(recoveryStrategyFailureCount);
                }
                TradeUtils.trendSet.clear();
                tradeService.modifyTrade(tradeEntity);
                // Note : in case of limit order no need to update price as its already set there.
                BigDecimal updatedMargin = marginService.getMargin(orderFeedWSMessage.getUserId()).subtract(openOrderEntity.getPrice()).multiply(new BigDecimal(Integer.parseInt(orderFeedWSMessage.getQuantity())));
                marginService.updateMarginInDB(orderFeedWSMessage.getUserId(), updatedMargin);
                updateOriginalOrderSLStatus(openOrderEntity);
                TradeUtils.slOrdersExecutionFlags.remove(openOrderEntity.getOriginalOrderId());
                // if there is no open trade it means no need of this Monitor  scheduler

                if (tradeEntity.getTradeStatus() == TradeStatus.COMPLETED && !tradeService.isOpenTradesFoundForToday()) {

                    List<TradeEntity> openTradesForInstrument = tradeService.getTradesByInstrumentAndStrategyAndStatus(openOrderEntity.getInstrumentToken(), openOrderEntity.getStrategyName());

                    if (openTradesForInstrument.isEmpty()) {
                        logger.info("All the trades are completed for the instrument {},  hence removing it from watch list", openOrderEntity.getInstrumentToken());
                        UserEntity userEntity = userService.getUserByUserId(openOrderEntity.getUserId());
                        String message = MessageFormat.format("Sell order feed: order id {0} with price {1}, quantity {2} and pnl {3}", Long.toString(openOrderEntity.getOrderId()), orderFeedWSMessage.getPrice(), orderFeedWSMessage.getQuantity(), realisedPnl);
                        notificationService.sendTelegramNotification(userEntity.getTelegramChatId(), message);

                        Optional<InstrumentWatchEntity> instrumentWatchEntity = instrumentWatchMasterRepo.findByInstrumentToken(openOrderEntity.getInstrumentToken());
                        instrumentWatchEntity.ifPresent(watchEntity -> {
                            instrumentWatchMasterRepo.delete(watchEntity);
                            TradeUtils.activeInstruments.remove(watchEntity.getInstrumentToken());
                        });

                        logger.info("Before consecutiveSuccessOrder");
                        List<TradeEntity> allTodaysTradesOrderByDesc = tradeService.getAllTodaysTradesOrderByDesc(openOrderEntity.getUserId()).stream().filter(trade -> !trade.getTradeId().equals(tradeEntity.getTradeId())).collect(Collectors.toList());
                        String configString = commonUtils.getConfigValue(ConfigConstants.FNO_CONSECUTIVE_SUCCESS_ORDER_COUNT);
                        int consecutiveSuccessOrderCount = Objects.isNull(configString) ? 3 : Integer.parseInt(configString);
                        if (!CollectionUtils.isEmpty(allTodaysTradesOrderByDesc) && allTodaysTradesOrderByDesc.size() >= consecutiveSuccessOrderCount) {
                            boolean consecutiveSuccessFlag = true;
                            for (int i = 0; i < consecutiveSuccessOrderCount; i++) {
                                if ("SUCCESSFUL".equals(allTodaysTradesOrderByDesc.get(i).getSuccessStatus())) {
                                    consecutiveSuccessFlag = consecutiveSuccessFlag && Boolean.TRUE;
                                } else {
                                    consecutiveSuccessFlag = false;
                                }
                            }
                            logger.info("inside consecutiveSuccessOrder");
                            if (consecutiveSuccessFlag && "UNSUCCESSFUL".equals(tradeEntity.getSuccessStatus())) {
                                TradeUtils.failureAfterConsecutiveSuccess.put(openOrderEntity.getUserId(), Boolean.TRUE);
                            }
                        }

                        if (CommonUtils.getOffMarketHoursTestFlag()) {
                            logger.info("Unsubscribe token {}", openOrderEntity.getInstrumentToken());
                        } else {
                            websocketUtils.unsubscribeAngelToken(openOrderEntity.getInstrumentToken());
                        }
                    }
                    TradeUtils.isLatestTrendFinderActive = true;
                    TradeUtils.isBigCandleProcessStart = false;
                    TradeUtils.removeInstrumentFromHighValues(openOrderEntity.getInstrumentToken());
                    TradeUtils.lastLtpTrendValue = BigDecimal.ZERO;
                    if (CommonUtils.getOffMarketHoursTestFlag()) {
                        CommonUtils.pauseMockWebsocket = false;
                    }
                    logger.info("After Trade Complete Flags are isLatestTrendFinderActive := {}, isBigCandleProcessStart := {} ,slOrdersExecutionFlags := {}, slOrderFallBackCallFlag := {},isWebsocketConnected {} ",
                            TradeUtils.isLatestTrendFinderActive, TradeUtils.isBigCandleProcessStart, TradeUtils.slOrdersExecutionFlags, TradeUtils.slOrderFallBackCallFlag, TradeUtils.isWebsocketConnected);
                }
            }
        }
    }

    private TradeEntity updateTradeDetails(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity openOrderEntity, Optional<TradeEntity> currentTrade, BigDecimal realisedPnl) {
        TradeEntity tradeOrderEntity = currentTrade.get();
        if (tradeOrderEntity.getTradeStatus() != TradeStatus.OPEN) {
            logger.info("Trade ID := {} status is Non Open, Hence returning from addSLAndMarginForPlacedOrder flow", tradeOrderEntity.getTradeId());
            return null;
        }
        if (tradeOrderEntity.getRealisedPnl() == null) {
            tradeOrderEntity.setRealisedPnl(BigDecimal.ZERO);
        }

        tradeOrderEntity.setRealisedPnl(tradeOrderEntity.getRealisedPnl().add(realisedPnl));
        updateTradeSuccessStatus(tradeOrderEntity);


        if (openOrderEntity.getTransactionType() == TransactionType.SELL) {
            //int remainingQuantity = tradeOrderEntity.getSellOpenQuantityToSquareOff() - Integer.parseInt(orderFeedWSMessage.getQuantity());
            tradeOrderEntity.setTradeStatus(TradeStatus.COMPLETED);
            tradeOrderEntity.setSellOpenQuantityToSquareOff(0);
        } else {
            int remainingQuantity = tradeOrderEntity.getBuyOpenQuantityToSquareOff() - Integer.parseInt(orderFeedWSMessage.getQuantity());
            tradeOrderEntity.setTradeStatus(remainingQuantity == 0 ? TradeStatus.COMPLETED : TradeStatus.EXECUTED);
            tradeOrderEntity.setBuyOpenQuantityToSquareOff(remainingQuantity);
        }

        tradeOrderEntity.setUpdatedTime(DateUtils.getCurrentDateTimeIst());
        if (tradeOrderEntity.getTradeStatus() == TradeStatus.COMPLETED) {
            long tradeDuration = tradeOrderEntity.getCreatedTime().until(tradeOrderEntity.getUpdatedTime(), ChronoUnit.SECONDS);
            tradeOrderEntity.setTradeDuration(tradeDuration);
            logger.info("Clearing Big Candle data to reanalyze the trend");
            candleService.clearTrendData();
        }
        return tradeOrderEntity;
    }

    private Integer updateUserStrategyDetails(OpenOrderEntity openOrderEntity, BigDecimal pnlPoints, BigDecimal buyPrice, String sellPrice) {
        UserStrategyEntity userStrategyEntity = userStrategyService.findStrategyByUserIdAndStrategy(openOrderEntity.getUserId(), openOrderEntity.getStrategyName());
        Integer recoveryStrategyFailureCount = 0;
        if (Objects.nonNull(userStrategyEntity)) {
            BigDecimal recoverQuantityPnl = new BigDecimal(TradeUtils.lossRecoveryQuantity).multiply(pnlPoints);
            BigDecimal overallLoss = userStrategyEntity.getOverallLoss();
            if (recoverQuantityPnl.compareTo(BigDecimal.ZERO) < 0) {
                userStrategyEntity.setOverallLoss(overallLoss.add(recoverQuantityPnl.abs()));
                userStrategyEntity.setFailureCount(userStrategyEntity.getFailureCount() + 1);
                logger.info("Recovery strategy :: Current order is failed with loss {} and failure count {}, hence adding it to over all loss {}. Overall loss after adding current loss {}", recoverQuantityPnl, userStrategyEntity.getFailureCount(), overallLoss, userStrategyEntity.getOverallLoss());
            } else if (userStrategyEntity.getOverallLoss().compareTo(recoverQuantityPnl) > 0) {
                userStrategyEntity.setOverallLoss(overallLoss.subtract(recoverQuantityPnl));
                userStrategyEntity.setFailureCount(userStrategyEntity.getFailureCount() + 1);
                logger.info("Recovery strategy :: Current order profit {} is less than over all loss {} with failure count {}, hence subtracting it from over all loss. Overall loss after subtracting current loss {}", recoverQuantityPnl, overallLoss, userStrategyEntity.getFailureCount(), userStrategyEntity.getOverallLoss());
            } else {
                recoveryStrategyFailureCount = userStrategyEntity.getFailureCount();
                userStrategyEntity.setOverallLoss(BigDecimal.ZERO);
                userStrategyEntity.setFailureCount(0);
                TradeUtils.lossRecoveryQuantity = userStrategyEntity.getQuantity();
                logger.info("Recovery strategy :: Current order profit {} is greater than over all loss {}, hence resetting the over all loss and failure count to 0", recoverQuantityPnl, overallLoss);
            }

            BigDecimal pnl = new BigDecimal(userStrategyEntity.getQuantity()).multiply(pnlPoints);
            userStrategyEntity.setOverallProfit(userStrategyEntity.getOverallProfit().add(pnl));
            BigDecimal newProfitPercent = userStrategyEntity.getDayProfitPercent().add(TradeUtils.getPercent(buyPrice, new BigDecimal(sellPrice)));
            userStrategyEntity.setDayProfitPercent(newProfitPercent);
            userStrategyEntity.setLastOrderQuantity(TradeUtils.lossRecoveryQuantity);

        }
        userStrategyService.saveUserStrategy(userStrategyEntity);
        return recoveryStrategyFailureCount;
    }

    private void updateTradeSuccessStatus(TradeEntity tradeOrderEntity) {
        List<OpenOrderEntity> orderEntities = tradeService.getOpenOrderList(tradeOrderEntity.getTradeId());
        if (!CollectionUtils.isEmpty(orderEntities)) {
            Optional<OpenOrderEntity> first = orderEntities.stream().filter(o -> o.getOrderStatus() == com.trade.algotrade.enums.OrderStatus.OPEN).findFirst();
            if (first.isPresent() && tradeOrderEntity.getTradeStatus() != TradeStatus.COMPLETED) {
                if (tradeOrderEntity.getRealisedPnl().compareTo(BigDecimal.ZERO) >= 0) {
                    tradeOrderEntity.setSuccessStatus(Constants.TRADE_SUCCESSFUL);
                } else {
                    tradeOrderEntity.setSuccessStatus(Constants.TRADE_UNSUCCESSFUL);
                }
            }
        }
    }

    private void updateOriginalOrderSLStatus(OpenOrderEntity openOrderEntity) {
        com.trade.algotrade.enums.OrderStatus oldStatus = openOrderEntity.getOrderStatus();
        openOrderEntity.setOrderStatus(com.trade.algotrade.enums.OrderStatus.EXECUTED);
        if (openOrderEntity.getOrderType() == OrderType.SL) {
            openOrderEntity.setOrderType(OrderType.MARKET);
            openOrderEntity.setSquareOffReason(SLTriggerReasonEnum.EXECUTE_SL.toString());
        }
        orderService.saveOpenOrders(openOrderEntity);
        logger.info("Updated Order Type := {} order status from OLD Status := {} to new Status := {} for ORDER ID:={}", openOrderEntity.getOrderType(), oldStatus, openOrderEntity.getOrderStatus(), openOrderEntity.getOrderId());
        Optional<TradeEntity> tradeEntity = tradeService.getTradeById(openOrderEntity.getTradeId());
        if (tradeEntity.isPresent()) {
            TradeEntity tradeOrderEntity = tradeEntity.get();
            List<OpenOrderEntity> orderEntities = tradeService.getOpenOrderList(tradeOrderEntity.getTradeId());
            orderEntities.stream().filter(order -> Objects.equals(order.getOrderId(), openOrderEntity.getOrderId())).forEach(order -> {
                order.setOrderStatus(com.trade.algotrade.enums.OrderStatus.EXECUTED);
                order.setIsSlOrderPlaced(Boolean.TRUE);
                order.setUpdatedAt(DateUtils.getCurrentDateTimeIst());
            });
            tradeService.createTrade(tradeOrderEntity);
        }
    }

/*

    private boolean prepareAndPlaceSlOrder(OrderFeedWSMessage orderFeedWSMessage, OpenOrderEntity openOrderEntity) {
        logger.info("Preparing and placing SL order for ORDER ID := {}", openOrderEntity.getOrderId());

        OrderRequest orderRequest = OrderRequest.builder()
                .instrumentToken(openOrderEntity.getInstrumentToken())
                .orderType(OrderType.SL)
                .quantity(openOrderEntity.getQuantity())
                .optionType(openOrderEntity.getOptionType())
                .strategyName(openOrderEntity.getStrategyName())
                .originalOrderId(Long.valueOf(orderFeedWSMessage.getOrderId()))
                .orderCategory(OrderCategoryEnum.SQAREOFF)
                .userIds(List.of(openOrderEntity.getUserId()))
                .segment(openOrderEntity.getSegment()).build();

        SlDetails slDetails = openOrderEntity.getSlDetails();
        if (slDetails != null) {
            BigDecimal slPercent = slDetails.getSlPercent();
            BigDecimal defaultSlPoint = slDetails.getSlPoints();
            BigDecimal buyPrice = new BigDecimal(orderFeedWSMessage.getPrice());

            BigDecimal slPrice = buyPrice.add(new BigDecimal(commonUtils.getConfigValue(ConfigConstants.SCALPING_TARGET_POINTS)).add(BigDecimal.ONE));
//            if (defaultSlPoint != null) {
//                slPrice = buyPrice.subtract(getValidSLPoints(buyPrice, defaultSlPoint));
//            }
//            // If slPercent is not null means we need to override it with percentage
//            if (slPercent != null && slPercent.compareTo(BigDecimal.ZERO) != 0) {
//                slPrice = buyPrice.subtract(getValidSLPoints(buyPrice, TradeUtils.getXPercentOfY(slPercent, buyPrice)));
//            }
//            // Still sl price is null means SL details are not defined for this order use min percent on buy price allowed by broker.
//            if (slPrice == null || slPrice.compareTo(BigDecimal.ZERO) == 0) {
//                BigDecimal validSLPoints = getValidSLPoints(buyPrice, null);
//                slPrice = buyPrice.subtract(validSLPoints);
//                slDetails.setSlPoints(validSLPoints);
//            }
//            // In case of Expiry strategy order consider default percent as SL
//            if (openOrderEntity.getStrategyName().equalsIgnoreCase(Strategy.EXPIRY.toString())) {
//                slPrice = buyPrice.subtract(TradeUtils.getXPercentOfY(Constants.DEFAULT_SL_FIFTY_PERCENT, buyPrice));
//            }
            slDetails.setTriggerPrice(slPrice.subtract(BigDecimal.ONE));
            slDetails.setTrailSl(slDetails.isTrailSl());
            slDetails.setTrailingStartPrice(buyPrice.add(slDetails.getSpread()));
            slDetails.setSlNextTargetPrice(buyPrice.add(slDetails.getSpread()));
            slDetails.setSpread(slDetails.getSpread());
            orderRequest.setPrice(slPrice);
            orderRequest.setSlDetails(slDetails);
            slDetails.setSlPrice(orderRequest.getPrice().subtract(BigDecimal.ONE));
        }
        orderRequest.setSlDetails(slDetails);
        orderRequest.setTransactionType(inverseTransactionType(openOrderEntity));
        List<OrderResponse> orderResponses = orderService.prepareOrderAndCreateOrder(orderRequest);
        if (CollectionUtils.isEmpty(orderResponses)) {
            logger.info("SL is not placed for ORDER ID :={}", openOrderEntity.getOrderId());
            return false;
        } else {
            //TODO check remove later
            TradeUtils.slOrdersExecutionFlags.put(Long.valueOf(orderFeedWSMessage.getOrderId()), Boolean.FALSE);
            logger.info("SL is placed for ORDER ID :={}", openOrderEntity.getOrderId());
            orderResponses.forEach(order -> TradeUtils.slOrderFallBackCallFlag.put(order.getOrderId(), Boolean.TRUE));
            websocketUtils.addAndSubscribeInstrument(orderRequest.getInstrumentToken());
            return true;
        }
    }
*/


    private BigDecimal getValidSLPoints(BigDecimal buyPrice, BigDecimal slBufferPoints) {
        String marketSLPercent = commonUtils.getConfigValue(ConfigConstants.MARKET_MIN_SL_PERCENT);
        if (marketSLPercent == null) {
            throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
        }
        BigDecimal allowedSLPrice = new BigDecimal(marketSLPercent).divide(new BigDecimal(100), 2, RoundingMode.CEILING).multiply(buyPrice);
        if (Objects.isNull(slBufferPoints)) {
            return allowedSLPrice;
        }
        return slBufferPoints.compareTo(allowedSLPrice) <= 0 ? slBufferPoints : allowedSLPrice;
    }

    private TransactionType inverseTransactionType(OpenOrderEntity openOrderEntity) {
        return openOrderEntity.getTransactionType() == TransactionType.BUY ? TransactionType.SELL : TransactionType.BUY;
    }

    //If SL order is not placed for buy order then after 8 Sec this method will place SL order - one time call.
    private boolean placeSlOrderFallBack(OrderResponse response) {

        OpenOrderEntity openOrderEntity = orderService.getSLOrderByOriginalOrderId(response.getOrderId());
        if (Objects.isNull(openOrderEntity)) {

            logger.info("SL order is not placed, hence placing SL order for order id {}", response.getOrderId());
            OpenOrderEntity orderEntity = orderService.findOrderByOrderId(response.getOrderId());
            if (Objects.nonNull(orderEntity)) {
                logger.info("Original Order Found in DB, Hence Proceeding fallback SL Order for Order ID {}", response.getOrderId());
                if (orderEntity.getOrderCategory().equals(OrderCategoryEnum.NEW)) {
                    List<OrderResponse> allOrders = orderService.getAllOrders(orderEntity.getUserId());
                    if (CommonUtils.getOffMarketHoursTestFlag()) {
                        allOrders.get(0).setOrderId(orderEntity.getOrderId());
                        allOrders.get(0).setInstrumentToken(orderEntity.getInstrumentToken());
                    }
                    Optional<OrderResponse> optionalKotakOrder = allOrders.stream().filter(order -> order.getOrderId().equals(orderEntity.getOrderId())).findFirst();
                    if (optionalKotakOrder.isPresent()) {
                        //Getting LTP to check whether current LTP is less than expected SL price.
                        OrderResponse orderResponse = optionalKotakOrder.get();
                        logger.info("Angelone Order Data Fetched ORDER ID {}, ORDER DATA {}", response.getOrderId(), orderResponse);
                        Long instrumentToken = orderEntity.getInstrumentToken();
                        QouteResponse ltp = angelOneClient.getQuote(ExchangeType.NFO, List.of(String.valueOf(instrumentToken)));
                        logger.info("LTP FETCHED FOR TOKEN {}, ORDER ID {}", response.getOrderId(), orderEntity.getInstrumentToken());
                        BigDecimal orderPrice = orderResponse.getPrice();
                        if (Objects.nonNull(orderPrice) && ltp != null && ltp.getData() != null && !CollectionUtils.isEmpty(ltp.getData().getFetched())) {
                            orderEntity.setPrice(orderPrice);
                            BigDecimal lastPrice = ltp.getData().getFetched().stream().findFirst().get().getLtp();
                            //If current LTP is less than expected SL price, set main order feed price based on the current LTP minus 10.
                            if (lastPrice.compareTo(orderPrice.subtract(orderEntity.getSlDetails().getSlPoints())) < 0) {
                                orderPrice = lastPrice.add(orderEntity.getSlDetails().getSlPoints().subtract(BigDecimal.TEN));
                            }
                        }
                        OrderFeedWSMessage message = OrderFeedWSMessage.builder().orderId(String.valueOf(orderEntity.getOrderId())).userId(orderEntity.getUserId()).price(String.valueOf(orderPrice)).quantity(String.valueOf(response.getQuantity())).build();
                        handleNewOrdersSlAndMargin(message, orderEntity);
                    }
                }
            }
        }
        List<OpenOrderEntity> ordersByIsSLPlaced = orderService.getOrdersByIsSLPlaced(false);

        if (CollectionUtils.isEmpty(ordersByIsSLPlaced)) {
            return true;
        }
        if (DateUtils.isBeforeTime(commonUtils.getConfigValue(ConfigConstants.TRADING_SESSION_START_TIME)) || DateUtils.isAfterTime(commonUtils.getConfigValue(ConfigConstants.TRADING_SESSION_END_TIME))) {
            // Don't trade before 10 AM and After 3.15 PM, in the morning
            logger.info("Stopping SL Monitoring Thread for strategy := {}, MARKET IS CLOSED NOW", Strategy.BIGCANDLE);
            return true;
        }
        return false;
    }

    //If SL order not placed for buy order then this thread will check on interval if SL order placed or not for that buy order.
    //If SL order is not placed then place the SL order.
    private void runSLExecuteVerificationThread(OrderResponse orderResponse) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Timer timer = new Timer();
        String tradeMonitorIntervalInMinutes = commonUtils.getConfigValue(ConfigConstants.FNO_SL_MONITORING_THREAD_DURATION);
        if (tradeMonitorIntervalInMinutes == null) {
            tradeMonitorIntervalInMinutes = "15"; // Default Interval is 15 min
        }
        long interval = Long.parseLong(tradeMonitorIntervalInMinutes) * 1000 * 60;
        long delay = 1000 * 15;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    future.complete(placeSlOrderFallBack(orderResponse));
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            }
        }, delay, interval);
        future.thenAccept(isThreadWorkCompleted -> {
            if (isThreadWorkCompleted) {
                logger.info("runSLExecuteVerificationThread Order Response : NO OPEN TRADE FOUND, HENCE CANCELLING TRADE MONITORING TASK");
                timer.cancel();
            }
        }).join();
    }

    // If SL order executed at kotak and we didn't received the notification
    // then check on interval if trade and order details are updated in database.
    private void runUpdateSLOrderInTreadThread(OpenOrderEntity openOrderEntity) {
        logger.info("Thread to Update DB details in case of ORDER FEED NOT Received for Order ID {}", openOrderEntity.getOrderId());
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Timer timer = new Timer();
        String tradeMonitorIntervalInMinutes = commonUtils.getConfigValue(ConfigConstants.FNO_SL_MONITORING_THREAD_DURATION);
        if (tradeMonitorIntervalInMinutes == null) {
            tradeMonitorIntervalInMinutes = "15"; // Default Interval is 15 min
        }
        long interval = Long.parseLong(tradeMonitorIntervalInMinutes) * 1000 * 60;
        long delay = 3000;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    future.complete(updateSLOrderAndTradeStatusFallBack(openOrderEntity));
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            }
        }, delay, interval);
        future.thenAccept(isThreadWorkCompleted -> {
            if (isThreadWorkCompleted) {
                logger.info("runUpdateSLOrderInTreadThread : NO OPEN TRADE FOUND, HENCE CANCELLING TRADE MONITORING TASK");
                timer.cancel();
            }
        }).join();
    }

    private void checkMonitorFlowStarted(OpenOrderEntity orderEntity) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Timer timer = new Timer();
        String checkMonitorIntervalInMinutes = "1";
        long interval = Long.parseLong(checkMonitorIntervalInMinutes) * 1000 * 30;
        long delay = 20000;
        logger.info("Setting timer to check is monitor flow started");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Boolean flag = TradeUtils.isMonitorFLowStartedForSLOrder.get(orderEntity.getOrderId());
                    if (Boolean.FALSE == flag) {
                        logger.info("Monitor flow not started for order {}, hence squaring off this order for flag {}", orderEntity.getOrderId(), flag);
                        if (modifySlOrLimitOrderToMarketOrder(orderEntity, SLTriggerReasonEnum.NO_MONITOR_SQUARE_OFF)) {
                            TradeUtils.isMonitorFLowStartedForSLOrder.remove(orderEntity.getOrderId());
                            future.complete(true);
                            timer.cancel();
                        }
                    }
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            }
        }, delay, interval);
    }

    private void checkLimitOrderIsOpen(Long orderId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Timer timer = new Timer();
        String checkMonitorIntervalInMinutes = "1";
        long interval = Long.parseLong(checkMonitorIntervalInMinutes) * 1000 * 60;
        long delay = 30000;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    OpenOrderEntity orderEntity = orderService.findOrderByOrderId(orderId);
                    if (com.trade.algotrade.enums.OrderStatus.OPEN.equals(orderEntity.getOrderStatus())) {
                        orderService.cancelOrder(orderEntity.getOrderId(), orderEntity.getUserId());
                        future.complete(true);
                        timer.cancel();
                    }
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            }
        }, delay, interval);
    }

    @Override
    public void buildBigMoveStrategy() {
        AngelOneInstrumentMasterEntity angelToken = instrumentService.findByInstrumentName(Constants.BANK_NIFTY_INDEX);
        Long bnfIndex = angelToken.getInstrumentToken();
        logger.info("BIG MOVE STRATEGY FLOW  CALLED BNF TOKEN {}", bnfIndex);
        GetCandleRequest angelOneCandleRequest = GetCandleRequest.builder().exchange(AngelOneExchange.NSE.toString()).fromdate(DateUtils.getTodayIst().plusDays(-5).format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT))).todate(DateUtils.getTodayIst().plusDays(1).format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT))).interval(CandleTimeFrame.ONE_DAY.toString()).symboltoken(bnfIndex.toString()).build();
        List<CandleEntity> candles = angelCandleService.getCandleData(angelOneCandleRequest);
        if (!CollectionUtils.isEmpty(candles)) {
            List<CandleEntity> bigCandles = sortCandlesDecending(candles);
            CandleEntity previousSessionDay = bigCandles.get(candles.size() - 2);
            String configValue = commonUtils.getConfigValue(ConfigConstants.BIG_MOVE_THRESHOLD);
            Integer bigMoveThreshold = configValue != null ? Integer.parseInt(configValue) : 400;
            QouteResponse ltp = angelOneClient.getQuote(com.trade.algotrade.client.angelone.enums.ExchangeType.NSE, List.of(String.valueOf(bnfIndex)));
            BigDecimal todayPrice = ltp.getData().getFetched().get(0).getLtp();
            logger.info("BIG MOVE STRATEGY DATA : YESTERDAY CLOSE {} , TODAY LTP AT 11 AM {}  BNF TOKEN {}", previousSessionDay.getClose(), todayPrice, bnfIndex);
            BigDecimal difference = todayPrice.subtract(previousSessionDay.getClose());
            if (difference.abs().compareTo(new BigDecimal(bigMoveThreshold)) > 0) {
                logger.info("BIG STRATEGY SETUP IS READY, SENDING ALERT");
                Optional<UserResponse> userResponseOptional = userService.getAllActiveUsersByBroker(BrokerEnum.ANGEL_ONE.toString()).stream().filter(userResponse -> userResponse.getSystemUser()).findFirst();
                String message = MessageFormat.format("BIG-MOVE Strategy ORDER Initiated : CHANGE {0}, DIRECTION {1},BNF LTP {}", difference, difference.compareTo(BigDecimal.ZERO) > 0 ? "UP" : "DOWN", todayPrice);
                notificationService.sendTelegramNotification(userResponseOptional.get().getTelegramChatId(), message);
                return;
            }
            logger.info("THERE IS NO BIG-MOVE STRATEGY SETUP FOUND");
        }
    }

    private static List<CandleEntity> sortCandlesDecending(List<CandleEntity> candles) {
        List<CandleEntity> sortedCandles = candles.stream().sorted(new Comparator<CandleEntity>() {
            @Override
            public int compare(CandleEntity o1, CandleEntity o2) {
                return o2.getCandleStart().compareTo(o1.getCandleStart());
            }
        }).collect(Collectors.toList());
        return sortedCandles;
    }

    private void updateTradeIfOrderRejected(OpenOrderEntity openOrderEntity) {
        openOrderEntity.setOrderStatus(com.trade.algotrade.enums.OrderStatus.REJECTED);
        orderService.saveOpenOrders(openOrderEntity);
        Optional<TradeEntity> optionalTrade = tradeService.getTradeForOrder(openOrderEntity.getOrderId().toString());
        if (optionalTrade.isPresent()) {
            TradeEntity tradeEntity = optionalTrade.get();
            tradeEntity.setTradeStatus(TradeStatus.REJECTED);
            tradeEntity.setSuccessStatus(Constants.TRADE_UNSUCCESSFUL);
        }
    }

    @Override
    public List<OrderResponse> createManualOrder(ManualOrderRequest orderRequest) {
        Strategy strategyName = Strategy.fromValue(orderRequest.getStrategyName().toString());
        StrategyResponse strategy;
        try {
            strategy = strategyService.getStrategy(strategyName.toString());
        } catch (AlgotradeException algotradeException) {
            logger.info("No Configuration found for the strategy := {} , Error details: {} Hence Returning.", strategyName, algotradeException.getError());
            throw new AlgotradeException(ErrorCodeConstants.STRATEGY_NOT_FOUND);
        }


        List<AngelOneInstrumentMasterEntity> allInstruments = instrumentService.getAllInstruments();
        Optional<AngelOneInstrumentMasterEntity> bnfInstrument = allInstruments.stream().filter(f -> Constants.BANK_NIFTY_INDEX.equalsIgnoreCase(f.getInstrumentName())).findFirst();
        if (bnfInstrument.isEmpty()) {
            logger.info("BNF INDEX Instrument not found for strategyName {}", Constants.BANK_NIFTY_INDEX);
            throw new AlgotradeException(ErrorCodeConstants.STRATEGY_NOT_FOUND);
        }

        AngelOneInstrumentMasterEntity angelToken = bnfInstrument.get();
        Long bnfIndex = angelToken.getInstrumentToken();
        QouteResponse ltp = angelOneClient.getQuote(com.trade.algotrade.client.angelone.enums.ExchangeType.NSE, List.of(String.valueOf(bnfIndex)));
        BigDecimal bnfLtp = ltp.getData().getFetched().get(0).getLtp();
        List<Integer> tradeStrikePrice = TradeUtils.getClosestStrikePrices(bnfLtp);
        OptionType orderOptionType;
        Optional<AngelOneInstrumentMasterEntity> orderInstrument;
        if (OptionType.CE == orderRequest.getOptionType()) {
            orderOptionType = OptionType.CE;
            orderInstrument = allInstruments.stream().filter(i -> tradeStrikePrice.get(1).equals(i.getStrike()) && OptionType.CE.toString().equals(i.getOptionType())).findAny();
        } else {
            orderInstrument = allInstruments.stream().filter(i -> tradeStrikePrice.get(0).equals(i.getStrike()) && OptionType.PE.toString().equals(i.getOptionType())).findAny();
            orderOptionType = OptionType.PE;
        }
        if (orderInstrument.isEmpty()) {
            logger.info("Empty Instrument Found for Instrument {} ,Hence Returning ****", orderInstrument);
            resetTrendFinderFlags();
            throw new AlgotradeException(ErrorCodeConstants.EMPTY_INSTRUMENTS_ERROR);
        }
        AngelOneInstrumentMasterEntity instrumentMasterEntity = orderInstrument.get();
        instrumentMasterEntity.setOptionType(orderOptionType.name());
        List<UserResponse> allFnoActiveUsers = userService.getAllActiveSegmentEnabledUsers(Segment.FNO);
        if (CollectionUtils.isEmpty(allFnoActiveUsers)) {
            logger.info("No Active user found for FNO segment to process {} strategy order,Hence Returning ", strategyName);
            throw new AlgotradeException(ErrorCodeConstants.NO_ACTIVE_USER_ERROR);
        }
        List<String> userIds = allFnoActiveUsers.stream().map(UserResponse::getUserId).collect(Collectors.toList());
        List<ValidationError> validationErrors = validateManualTradingRules(strategyName, userIds);
        if (!CollectionUtils.isEmpty(validationErrors)) {
            throw new AlgoValidationException(new BusinessError(HttpStatus.BAD_REQUEST, validationErrors));
        }
        List<TradeEntity> allOpenTrades = tradeService.getTodaysOpenTradesByStrategyAndUserIdIn(strategyName.toString(), userIds);
        List<String> nonOpenTradeUserIds = userIds.stream().filter(u -> allOpenTrades.stream().anyMatch(ao -> ao.getUserId().equalsIgnoreCase(u))).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(allOpenTrades) && CollectionUtils.isEmpty(nonOpenTradeUserIds)) {
            logger.info("Strategy :=  {} Open Trades (Count := {}) Found, Hence Returning ", strategyName, allOpenTrades.size());
            throw new AlgotradeException(ErrorCodeConstants.OPEN_STRATEGY_ORDER_FOUND);
        }
        return processManualStrategyOrder(allFnoActiveUsers.get(0).getUserId(), instrumentMasterEntity, orderOptionType, strategy, false);
    }


    private List<ValidationError> validateManualTradingRules(Strategy strategyName, List<String> userIds) {
        List<ValidationError> validationErrors = new ArrayList<>();
        userIds.stream().forEach(user -> {
            UserStrategyEntity userStrategy = userStrategyService.findStrategyByUserIdAndStrategy(user, strategyName.toString());
            if (userStrategy == null) {
                logger.info("Strategy User configuration is missing for {}", strategyName);
                throw new AlgotradeException(ErrorCodeConstants.STRATEGY_NOT_FOUND);
            }
            BigDecimal dayProfitPercent = userStrategy.getDayProfitPercent();

            int maxProfitPercent = 20;
            boolean isNonZeroValue = dayProfitPercent.compareTo(BigDecimal.ZERO) != 0;
            if (isNonZeroValue && dayProfitPercent.compareTo(new BigDecimal(maxProfitPercent)) >= 0) {
                ValidationError violations = new ValidationError();
                violations.setField("dayProfitPercent");
                violations.setMessage(user + "> Day profit is reached " + maxProfitPercent + " %, No further orders allowed");
                violations.setRejectedValue(dayProfitPercent);
                validationErrors.add(violations);
            }

            int maxLossPercent = 10;
            if (isNonZeroValue && new BigDecimal(maxLossPercent).compareTo(dayProfitPercent.abs()) <= 0) {
                ValidationError violations = new ValidationError();
                violations.setField("dayProfitPercent");
                violations.setMessage(user + "> Day loss is reached " + maxLossPercent + " %  No further orders allowed");
                violations.setRejectedValue(dayProfitPercent);
                validationErrors.add(violations);
            }
        });
        return validationErrors;
    }

    public List<OrderResponse> processManualStrategyOrder(String userIds, AngelOneInstrumentMasterEntity
            instrumentMasterEntity, OptionType optionType, StrategyResponse strategy, boolean mockOrder) {
        logger.debug("MANUAL Strategy for all active users Strike Price : = {}  and for Option Type := {}", instrumentMasterEntity.getStrike(), optionType);
        OrderRequest orderRequest = getOrderRequest(userIds, instrumentMasterEntity, optionType);
        orderRequest.setMockOrder(mockOrder);
        ExitCondition exitCondition = strategy.getExitCondition();
        if (Objects.nonNull(exitCondition) && !CollectionUtils.isEmpty(exitCondition.getConditions())) {
            Map<String, String> conditions = exitCondition.getConditions();
            String stopLossPercentage = conditions.get(Constants.DEFAULT_SL_PERCENT);
            SlDetails slDetails = new SlDetails();
            if (StringUtils.isNotEmpty(stopLossPercentage)) {
                if (commonUtils.isTodayExpiryDay()) {
                    slDetails.setSlPercent(new BigDecimal(stopLossPercentage).multiply(new BigDecimal(2)));
                } else {
                    slDetails.setSlPercent(new BigDecimal(stopLossPercentage));
                }
            }
            String slBufferPercent = conditions.get(Constants.DEFAULT_SL_BUFFER_PERCENT);

            if (StringUtils.isNotEmpty(slBufferPercent)) {
                slDetails.setSlBufferPercent(new BigDecimal(slBufferPercent));
            }
            String targetPercent = conditions.get(Constants.DEFAULT_TARGET_PERCENT);
            if (StringUtils.isNotEmpty(targetPercent)) {
                if (commonUtils.isTodayExpiryDay()) {
                    slDetails.setTargetPercent(new BigDecimal(targetPercent).multiply(new BigDecimal(5)));
                } else {
                    slDetails.setTargetPercent(new BigDecimal(targetPercent));
                }
            }
            orderRequest.setSlDetails(slDetails);
        }
        if (!mockOrder) {
            setSwingLowAsSL(instrumentMasterEntity, orderRequest);
        }
        logger.info("MANUAL Strategy > Placing order for all active users Strategy := {},Strike Price  : = {} and Option Type := {}", strategy.getStrategyName(), instrumentMasterEntity.getStrike(), optionType);
        return prepareStrategyOrder(orderRequest, strategy);
    }

    private void setSwingLowAsSL(AngelOneInstrumentMasterEntity instrumentMasterEntity, OrderRequest orderRequest) {
        Long instrumentToken = instrumentMasterEntity.getInstrumentToken();
        GetCandleRequest premiumCandles =
                GetCandleRequest.builder().exchange(AngelOneExchange.NSE.toString())
                        .fromdate(DateUtils.getTodayIst().plusMinutes(-15)
                                .format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT)))
                        .todate(DateUtils.getTodayIst()
                                .format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT)))
                        .interval(CandleTimeFrame.FIVE_MINUTE.toString()).symboltoken(instrumentToken.toString()).build();

        List<CandleEntity> candles = angelCandleService.getCandleData(premiumCandles);
        SlDetails slDetails = new SlDetails();
        if (!CollectionUtils.isEmpty(candles)) {
            List<CandleEntity> sortedCandles = candles.stream().sorted(new Comparator<CandleEntity>() {
                @Override
                public int compare(CandleEntity o1, CandleEntity o2) {
                    return o1.getLow().compareTo(o2.getLow());
                }
            }).collect(Collectors.toList());
            CandleEntity candleEntity = sortedCandles.get(0);
            slDetails.setSlPrice(candleEntity.getLow());
            orderRequest.setSlDetails(slDetails);
        }
    }

    private static OrderRequest getOrderRequest(String userId, AngelOneInstrumentMasterEntity
            kotakInstrumentMasterEntity, OptionType optionType) {
        OrderRequest orderRequest = OrderRequest.builder()
                .instrumentToken(kotakInstrumentMasterEntity.getInstrumentToken())
                .optionType(optionType)
                .orderCategory(OrderCategoryEnum.NEW)
                .segment(Segment.FNO.toString())
                .orderType(OrderType.MARKET)
                .transactionType(TransactionType.BUY)
                .userId(userId)
                .price(BigDecimal.ZERO).build();
        return orderRequest;
    }

    @Override
    public void processSLOrderFeed(OrderFeedWSMessage orderFeedWSMessage) {
        String orderFeedOrderId = orderFeedWSMessage.getOrderId();
        try {
            OpenOrderEntity orderByOrderId = orderService.findOrderByOrderId(Long.valueOf(orderFeedOrderId));
            Boolean islOrder = orderByOrderId.getIsSL();
            if (BooleanUtils.isTrue(islOrder) && OrderStatus.fromValue(orderFeedWSMessage.getStatus()) == OrderStatus.FIL) {
                logger.info("SL Order Executed at Broker {},Status {}", orderFeedOrderId, orderFeedWSMessage);
                processOrderFeed();
            }
        } catch (AlgoValidationException exception) {
            OrderFeedWSMessage queueMessage = TradeUtils.orderFeedQueue.poll();
            logger.error("Order not found in DB {} Hence removed unused order feed from queue  Total Messages in QUEUE {}, Error {}", orderFeedOrderId, TradeUtils.orderFeedQueue.size(), exception.getMessage());
        }
    }


    @Override
    public List<OrderResponse> prepareMockOrder(ManualOrderRequest orderRequest) {
        Strategy strategyName = Strategy.fromValue(orderRequest.getStrategyName().toString());
        StrategyResponse strategy;
        try {
            strategy = strategyService.getStrategy(strategyName.toString());
        } catch (AlgotradeException algotradeException) {
            logger.info("No Configuration found for the strategy := {} , Error details: {} Hence Returning.", strategyName, algotradeException.getError());
            throw new AlgotradeException(ErrorCodeConstants.STRATEGY_NOT_FOUND);
        }


        List<AngelOneInstrumentMasterEntity> allInstruments = instrumentService.getAllInstruments();
        Optional<AngelOneInstrumentMasterEntity> bnfInstrument = allInstruments.stream().filter(f -> Constants.BANK_NIFTY_INDEX.equalsIgnoreCase(f.getInstrumentName())).findFirst();
        if (bnfInstrument.isEmpty()) {
            logger.info("BNF INDEX Instrument not found for strategyName {}", Constants.BANK_NIFTY_INDEX);
            throw new AlgotradeException(ErrorCodeConstants.EMPTY_INSTRUMENTS_ERROR);
        }

        AngelOneInstrumentMasterEntity angelToken = bnfInstrument.get();
        Long bnfIndex = angelToken.getInstrumentToken();
        QouteResponse ltp = angelOneClient.getQuote(com.trade.algotrade.client.angelone.enums.ExchangeType.NSE, List.of(String.valueOf(bnfIndex)));
        BigDecimal bnfLtp = ltp.getData().getFetched().get(0).getLtp();
        List<Integer> tradeStrikePrice = TradeUtils.getClosestStrikePrices(bnfLtp);
        OptionType orderOptionType;
        Optional<AngelOneInstrumentMasterEntity> orderInstrument;
        if (OptionType.CE == orderRequest.getOptionType()) {
            orderOptionType = OptionType.CE;
            orderInstrument = allInstruments.stream().filter(i -> tradeStrikePrice.get(1).equals(i.getStrike()) && OptionType.CE.toString().equals(i.getOptionType())).findAny();
        } else {
            orderInstrument = allInstruments.stream().filter(i -> tradeStrikePrice.get(0).equals(i.getStrike()) && OptionType.PE.toString().equals(i.getOptionType())).findAny();
            orderOptionType = OptionType.PE;
        }
        if (orderInstrument.isEmpty()) {
            logger.info("Empty Instrument Found for Instrument {} ,Hence Returning ****", orderInstrument);
            resetTrendFinderFlags();
            throw new AlgotradeException(ErrorCodeConstants.EMPTY_INSTRUMENTS_ERROR);
        }
        AngelOneInstrumentMasterEntity instrumentMasterEntity = orderInstrument.get();
        instrumentMasterEntity.setOptionType(orderOptionType.name());
        List<UserResponse> allFnoActiveUsers = userService.getAllActiveSegmentEnabledUsers(Segment.FNO);
        if (CollectionUtils.isEmpty(allFnoActiveUsers)) {
            logger.info("No Active user found for FNO segment to process {} strategy order,Hence Returning ", strategyName);
            throw new AlgotradeException(ErrorCodeConstants.NO_ACTIVE_USER_ERROR);
        }
        List<String> userIds = allFnoActiveUsers.stream().map(UserResponse::getUserId).collect(Collectors.toList());
        List<ValidationError> validationErrors = validateManualTradingRules(strategyName, userIds);
        if (!CollectionUtils.isEmpty(validationErrors)) {
            throw new AlgoValidationException(new BusinessError(HttpStatus.BAD_REQUEST, validationErrors));
        }
        List<TradeEntity> allOpenTrades = tradeService.getTodaysOpenTradesByStrategyAndUserIdIn(strategyName.toString(), userIds);
        List<String> nonOpenTradeUserIds = userIds.stream().filter(u -> allOpenTrades.stream().anyMatch(ao -> ao.getUserId().equalsIgnoreCase(u))).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(allOpenTrades) && CollectionUtils.isEmpty(nonOpenTradeUserIds)) {
            logger.info("Strategy :=  {} Open Trades (Count := {}) Found, Hence Returning ", strategyName, allOpenTrades.size());
            throw new AlgotradeException(ErrorCodeConstants.OPEN_STRATEGY_ORDER_FOUND);
        }
        return processManualStrategyOrder(userIds.get(0), instrumentMasterEntity, orderOptionType, strategy, true);
    }
}
