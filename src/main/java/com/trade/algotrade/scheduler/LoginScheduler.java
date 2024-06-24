package com.trade.algotrade.scheduler;

import com.trade.algotrade.client.angelone.AngelOneClient;
import com.trade.algotrade.client.angelone.enums.AngelOneExchange;
import com.trade.algotrade.client.angelone.enums.CandleTimeFrame;
import com.trade.algotrade.client.angelone.request.GetCandleRequest;
import com.trade.algotrade.client.angelone.service.AngelCandleService;
import com.trade.algotrade.client.angelone.websocket.WebsocketInitializer;
import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.OrderFeedWsClientEndpoint;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.enitiy.TradeHistoryEntity;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.request.ManualOrderRequest;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.*;
import com.trade.algotrade.service.equity.StockMasterService;
import com.trade.algotrade.task.service.PeriodicOrderMonitoringService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@EnableAsync
public class LoginScheduler {

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    KotakClient kotakClient;

    @Autowired
    InstrumentService instrumentService;

    @Autowired
    CandleService candleService;

    @Autowired
    MarginService marginService;

    @Autowired
    StrategyBuilderService strategyBuilderService;

    @Autowired
    OrderService orderService;

    @Autowired
    TradeUtils tradeUtils;

    @Autowired
    MongoDumpService mongoDumpService;

    @Autowired
    TradeService tradeService;

    @Autowired
    AngelOneClient angelOneClient;

    @Autowired
    StockMasterService stockMasterService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    UserService userService;


    @Autowired
    AngelCandleService angelCandleService;

    private final Logger logger = LoggerFactory.getLogger(LoginScheduler.class);

    @Scheduled(cron = "${scheduler.kotak-login.interval}")
    @Async
    public void getAccessToken() {
        logger.info(" ****** [ Kotak Login ] [getKotakLogin ] Started ********");
        kotakClient.getSessionToken(false);
        userService.getAllActiveUsersByBroker(BrokerEnum.KOTAK_SECURITIES.toString()).forEach(userResponse -> notificationService.sendTelegramNotification(userResponse.getTelegramChatId(), MessageFormat.format("Login successful for user {0}", userResponse.getUserId())));
        //Generating time frame slots for candle.
        candleService.generateTimeSlotUsingTimeFrame();
        logger.info(" ****** [  Kotak Login ] [getKotakLogin ] Completed ********");
    }

    @Scheduled(cron = "${scheduler.angelone-login.interval}")
    @Async
    public void getAngelOneAccessToken() {
        logger.info(" ****** [ Angel One Login ] [getAngelOneAccessToken ] Started ********");
        angelOneClient.getAccessTokenForAllActiveUsers();
        logger.info(" ****** [ Angel One Login ] [ getAngelOneAccessToken ] Completed ********");
    }

    @Async
    @Scheduled(cron = "${scheduler.download-instrument.interval}")
    public void downloadAndSaveInstrument() {
        logger.info(" ****** [ Kotak Login ] [downloadAndSaveInstrument ] Started ********");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        instrumentService.downlodInstruments();
        //Check mongo db static tables if not available then restore
        mongoDumpService.restoreStaticTables();
        logger.info(" ****** [  Kotak Login ] [downloadAndSaveInstrument ] Completed ********");
    }


    @Async
    @Scheduled(cron = "${scheduler.margin.interval}")
    public void loadMarginDataLocally() {
        logger.info(" ****** [ Kotak Login ] [loadMarginDataLocally ] Started ********");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        marginService.getMarginForAllUser();
        logger.info(" ****** [  Kotak Login ] [loadMarginDataLocally ] Completed ********");
    }


    @Async
    @Scheduled(cron = "${scheduler.order-cache.interval}")
    public void loadOrderCacheData() {
        logger.info("LOAD ORDER CACHE BY PLACING MOCK ORDER > Called");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        strategyBuilderService.prepareMockOrder(ManualOrderRequest.builder().strategyName(ManualStrategy.MANUAL).optionType(OptionType.CE).build());
        logger.info("LOAD ORDER CACHE BY PLACING MOCK ORDER > Completed");
    }

    // @Scheduled(cron = "${scheduler.expiry-strategy.interval}")
    @Async
    public void buildExpiryStrategy() {
        logger.info(" ****** [ Kotak Login ] [buildExpiryStrategy ] Called ********");
        if (commonUtils.isTodayNseHoliday() && !commonUtils.isTodayExpiryDay()) {
            return;
        }
        LiveFeedWSMessage message = LiveFeedWSMessage.builder().instrumentName(Constants.BANK_NIFTY_INDEX).messageSource(MessageSource.MANULJOB).build();
        strategyBuilderService.buildBigCandleStrategy(message, Strategy.EXPIRY);
        logger.info(" ****** [  Kotak Login ] [buildExpiryStrategy ] Completed ********");
    }

    @Async
    public void moveCompletedTradesToHistory() {
        logger.info(" ****** [ Kotak Login ] [moveCompletedOrderToHistory ] Called ********");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        List<TradeHistoryEntity> tradeHistoryEntities = tradeService.moveTradesToHistory();
        if (!CollectionUtils.isEmpty(tradeHistoryEntities)) {
            orderService.moveOrdersToHistory(tradeHistoryEntities);
        }
        logger.info(" ****** [  Kotak Login ] [moveCompletedOrderToHistory ] Completed ********");
    }

    @Async
    // @Scheduled(fixedRate = 15000)
    public void fetchOrders() {
        logger.info(" ****** [ Kotak Login ] [fetchOrders ] Started ********");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        List<OrderResponse> allOrders = orderService.getOpenOrders();
        logger.info(" ****** [  Kotak Login ] [fetchOrders ] Completed ********");
    }

    @Scheduled(cron = "${scheduler.eod-process.interval}")
    public void eodProcessScheduler() {

        logger.info(" ****** [ Kotak Login ] [stop scheduler] Called ********");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }

        logger.info(" ****** Stopping Order Monitor Scheduler ********");
        OrderFeedWsClientEndpoint.stopOrderMonitoringScheduler();
        logger.info(" ****** Stopped Order Monitor Scheduler ********");


        logger.info(" ****** Moving Completed Trades to History Called  *****************");
        moveCompletedTradesToHistory();
        logger.info(" ****** Moving Completed Trades to History Called  *****************");

        logger.info(" ****** Equity Day Close called *****************");
        updateEquityDayClose();
        logger.info(" ****** Equity Day Close Completed *****************");

        logger.info(" ****** Log Out All Angel One users started *****************");
        angelOneClient.logoutAllActiveUsers();
        logger.info(" ****** Log Out All Angel One users completed ***************");

        logger.info(" ****** [ Kotak Login ] [clear static app data method] Called ********");
        clearStaticData();
        logger.info(" ****** [  Kotak Login ] [clear static app data method ] Completed ********");

        logger.info(" ****** [  Kotak Login ] [Mongo DB static table dump] Called ********");
        mongoDumpService.takeDumpOfStaticTables();
        logger.info(" ****** [  Kotak Login ] [Mongo DB static table dump] Completed ********");

    }

    private void clearStaticData() {
        tradeUtils.deleteAllWatchInstruments();
        //saveAndClearBankNiftyCandle();
        TradeUtils.getPremiumHighValues().clear();
        TradeUtils.getPremiumLowValues().clear();
        TradeUtils.isFirstCandleFlag = false;
        TradeUtils.getTimeFrameSlots().clear();
    }

    private void saveAndClearBankNiftyCandle() {
        logger.info(" ****** [ Kotak Login ] [saveAndClearBankNiftyCandle ] Called ********");

        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        List<CandleEntity> candles = candleService.findBySymbol(Constants.BANK_NIFTY_INDEX);
        if (!CollectionUtils.isEmpty(candles)) {

            // Find Candle which is not in DB
            List<CandleEntity> candlesPendingToSave = TradeUtils.getNiftyBankCandle().values().stream().filter(mapCandle -> {
                return candles.stream().anyMatch(c -> Objects.nonNull(c.getCandleStart()) && !c.getCandleStart().equalsIgnoreCase(mapCandle.getCandleStart()));
            }).collect(Collectors.toList());

            /**
             * If Candle found in map which is not there in DB , Update all those candles in
             * DB.
             */

            if (!CollectionUtils.isEmpty(candlesPendingToSave)) {
                candlesPendingToSave.stream().forEach(c -> {
                    try {
                        candleService.saveCandle(c);
                    } catch (DuplicateKeyException e) {
                        logger.debug("Ignoring DuplicateKeyException exception as data is already present in DB ");
                    }
                });
            }
        } else {
            TradeUtils.getNiftyBankCandle().values().stream().forEach(candle -> {
                candleService.saveCandle(candle);
            });
        }
        TradeUtils.getNiftyBankCandle().clear();
        logger.info(" ****** [ Kotak Login ] [saveAndClearBankNiftyCandle ] Completed ********");
    }

    //
    // @Scheduled(cron = "${scheduler.cancelExpiryOrders.interval}")
    @Async
    public void cancelOpenExpiryOrder() {
        logger.info(" ****** [ Kotak Login ] [cancelOpenExpiryOrder ] Called ********");
        if (commonUtils.isTodayNseHoliday() && !commonUtils.isTodayExpiryDay()) {
            return;
        }
        orderService.cancelExpiryOrder();
        logger.info(" ****** [  Kotak Login ] [cancelOpenExpiryOrder ] Completed ********");
    }


    @Async
    public void updateEquityDayClose() {
        logger.info(" ****** UPDATE NEWS SPIKE STOCK GAIN STARTED ***************");
        stockMasterService.updateNewsSpikeStockGain();
        logger.info(" ****** UPDATE NEWS SPIKE STOCK GAIN COMPLETED ***************");
        stockMasterService.updateDayClose();
    }


    @Scheduled(cron = "${scheduler.squareoffExpiryOrder.interval}")
    @Async
    public void squareOffFnoIntradayOrder() {
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        if (commonUtils.isTodayExpiryDay()) {
            orderService.squareOffOrder(Strategy.EXPIRY.toString());
        }
        orderService.squareOffOrder(Strategy.BIGCANDLE.toString());
        logger.info(" ****** [  Kotak Login ] [squareOffFnoIntradayOrder ] Completed ********");
    }

    @Async
    public void getAccessTokenManually() {
        logger.info(" ****** [ Kotak Login ] [getKotakLogin ] Started ********");
        kotakClient.getSessionToken(true);
        logger.info(" ****** [  Kotak Login ] [getKotakLogin ] Completed ********");
    }

    public void clearStaticAppDataScheduler() {
        clearStaticData();
    }

    @Async
    @Scheduled(cron = "${scheduler.bnf-spike-notification.interval}")
    public void bankNiftySpikeNotification() {
        logger.info("BNF SPIKE CANDLE:- NOTIFICATION CHECK ");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }

        // TODO Need to handle NIFTY SPIKE as well;

        AngelOneInstrumentMasterEntity kotakInstruments = instrumentService.findByInstrumentName(Constants.BANK_NIFTY_INDEX);
        Long bnfIndex = instrumentService.getInstrumentByToken(kotakInstruments.getInstrumentToken()).getInstrumentToken();
        GetCandleRequest angelOneCandleRequest = GetCandleRequest.builder().exchange(AngelOneExchange.NSE.toString())
                .fromdate(DateUtils.getTodayIst().format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT)))
                .todate(DateUtils.getTodayIst().plusDays(1).format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT)))
                .interval(CandleTimeFrame.FIVE_MINUTE.toString()).symboltoken(bnfIndex.toString()).build();

        logger.info("CANDLE DATA ANGELONE REQUEST {}", angelOneCandleRequest);
        List<CandleEntity> candles = angelCandleService.getCandleData(angelOneCandleRequest);
        logger.info("CANDLE DATA ANGELONE RESPONSE {}", candles);
        if (!CollectionUtils.isEmpty(candles)) {
            CandleEntity latestCandle = candles.get(candles.size() - 1);
            BigDecimal candleTotalPoints = latestCandle.getCandleTotalPoints();
            logger.info("CHECKING BNF SPIKE CANDLE {}", candleTotalPoints);
            if (candleTotalPoints.abs().compareTo(new BigDecimal(150)) > 0) {
                Optional<UserResponse> userResponseOptional = userService.getAllActiveUsersByBroker(BrokerEnum.ANGEL_ONE.toString()).stream().filter(userResponse -> userResponse.getSystemUser()).findFirst();
                if (userResponseOptional.isPresent()) {
                    String candleType;
                    if (latestCandle.getCandleBodyPoints().compareTo(BigDecimal.ZERO) > 0) {
                        candleType = "UP";
                    } else {
                        candleType = "DOWN";
                    }
                    notificationService.sendTelegramNotification(userResponseOptional.get().getTelegramChatId(), MessageFormat.format("SPIKE in BANKNIFTY Index Observed more than  {0} Points, In Direction of {1}", candleTotalPoints.abs(), candleType));
                }
            }
            List<CandleEntity> sortedCandles = candles.stream().limit(5).collect(Collectors.toList());
            CandleEntity firstCandle = sortedCandles.get(0);
            CandleEntity recentCandle = sortedCandles.get(sortedCandles.size() - 1);
            BigDecimal totalDifference = firstCandle.getOpen().subtract(recentCandle.getClose());
            if (totalDifference.abs().compareTo(new BigDecimal(300)) > 0) {
                Optional<UserResponse> userResponseOptional = userService.getAllActiveUsersByBroker(BrokerEnum.ANGEL_ONE.toString()).stream().filter(userResponse -> userResponse.getSystemUser()).findFirst();
                if (userResponseOptional.isPresent()) {
                    String candleType;
                    if (totalDifference.compareTo(BigDecimal.ZERO) > 0) {
                        candleType = "UP";
                    } else {
                        candleType = "DOWN";
                    }
                    notificationService.sendTelegramNotification(userResponseOptional.get().getTelegramChatId(), MessageFormat.format("BIG TREND FOUND in 25 Min in  BANKNIFTY Index Observed more than  {0} Points, In Direction of {1}", totalDifference.abs(), candleType));
                }
            }
        }
        //TODO Pivot and CPR notification on current candles.


        logger.info("BNF SPIKE CANDLE:- NOTIFICATION COMPLETED.");
    }


    @Async
    //@Scheduled(cron = "${scheduler.bnf-spike-notification.interval}")
    public void backTestingLogic() {
        logger.info(" ****** [BACK TESTING ] Called ********");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }


        AngelOneInstrumentMasterEntity kotakInstruments = instrumentService.findByInstrumentName(Constants.BANK_NIFTY_INDEX);
        Long bnfIndex = instrumentService.getInstrumentByToken(kotakInstruments.getInstrumentToken()).getInstrumentToken();
        GetCandleRequest angelOneCandleRequest = GetCandleRequest.builder().exchange(AngelOneExchange.NSE.toString()).fromdate(DateUtils.getTodayIst().plusDays(-2000).format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT))).todate(DateUtils.getTodayIst().plusDays(1).format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT))).interval(CandleTimeFrame.ONE_DAY.toString()).symboltoken(bnfIndex.toString()).build();
        List<CandleEntity> candles = angelCandleService.getCandleData(angelOneCandleRequest);

        if (!CollectionUtils.isEmpty(candles)) {
            List<CandleEntity> sortedCandles = candles.stream().sorted(new Comparator<CandleEntity>() {
                @Override
                public int compare(CandleEntity o1, CandleEntity o2) {
                    return o1.getCandleStart().compareTo(o2.getCandleStart());
                }
            }).collect(Collectors.toList());
            List<CandleEntity> bigCandles = IntStream.range(0, sortedCandles.size()).mapToObj(index -> {
                CandleEntity currentDay = sortedCandles.get(index);
                if (index > 0) {
                    CandleEntity previousDay = sortedCandles.get(index - 1);
                    currentDay.setPreviousClose(previousDay.getClose());
                } else {
                    currentDay.setPreviousClose(currentDay.getOpen());
                }
                return currentDay;
            }).collect(Collectors.toList());

            List<CandleEntity> gapOpenList = bigCandles.stream().filter(e -> {
                BigDecimal gapOpen = e.getPreviousClose().subtract(e.getOpen());
                gapOpen = gapOpen.setScale(2, RoundingMode.CEILING);
                int gapOpenThreshold = 100;
                if (gapOpen.abs().compareTo(new BigDecimal(gapOpenThreshold)) >= 0) {
                    e.setGapBody(gapOpen);
                    return true;
                }
                return false;
            }).collect(Collectors.toList());


            List<CandleEntity> bigMoves = bigCandles.stream().filter(e -> {
                BigDecimal bigBody = e.getPreviousClose().subtract(e.getClose());
                bigBody = bigBody.setScale(2, RoundingMode.CEILING);
                int bigMoveThreshold = 100;
                if (bigBody.abs().compareTo(new BigDecimal(bigMoveThreshold)) >= 0) {
                    e.setChangeBody(bigBody);
                    return true;
                }
                return false;
            }).collect(Collectors.toList());


            Optional<UserResponse> userResponseOptional = userService.getAllActiveUsersByBroker(BrokerEnum.KOTAK_SECURITIES.toString()).stream().filter(userResponse -> userResponse.getSystemUser()).findFirst();
            if (!CollectionUtils.isEmpty(bigCandles)) {
                logger.info(" ****** [BACK TESTING ] MAX Days {} ********", bigCandles.size());
                bigCandles.forEach(candleEntity -> {
                    String candleType;
                    if (candleEntity.getGapBody() != null) {
                        if (candleEntity.getGapBody().compareTo(BigDecimal.ZERO) > 0) {
                            candleType = "UP";
                        } else {
                            candleType = "DOWN";
                        }
                        String message = MessageFormat.format("BIG-MOVE : DATE {0}, CHANGE {1}, DIRECTION {2}",
                                candleEntity.getCandleStart().toString(), candleEntity.getGapBody(), candleType);
                        logger.info("DATA {} ", message);
                    /*    if (userResponseOptional.isPresent()) {
                            notificationService.sendTelegramNotification(userResponseOptional.get().getTelegramChatId(), message);
                        }*/
                    }
                });
            }
        }

        logger.info(" ****** [BACK TESTING ] Called ********");
    }
}