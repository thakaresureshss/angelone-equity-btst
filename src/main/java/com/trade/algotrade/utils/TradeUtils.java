package com.trade.algotrade.utils;

import com.trade.algotrade.constants.CharConstant;
import com.trade.algotrade.dto.OrderMessage;
import com.trade.algotrade.dto.websocket.OrderFeedWSMessage;
import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.enitiy.InstrumentWatchEntity;
import com.trade.algotrade.enitiy.equity.StockMaster;
import com.trade.algotrade.repo.InstrumentWatchMasterRepo;
import com.trade.algotrade.request.OrderRequest;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author suresh.thakare
 */
@Component
public class TradeUtils {
    private final static Logger logger = LoggerFactory.getLogger(TradeUtils.class);

    @Autowired
    InstrumentWatchMasterRepo instrumentWatchMasterRepo;

    private static final Map<String, CandleEntity> bankNiftyCandels = new HashMap<String, CandleEntity>();

    private static final Map<Long, BigDecimal> premiumHighValues = new HashMap<Long, BigDecimal>();

    private static final Map<Long, BigDecimal> premiumLowValues = new HashMap<Long, BigDecimal>();

    public static BigDecimal scalpDirecton = BigDecimal.ZERO;

    private static final Map<String, Map<String, CandleEntity>> stockCandles = new HashMap<String, Map<String, CandleEntity>>();

    public static boolean isFirstCandleFlag = false;

    private static Map<Integer, List<Integer>> timeFrameSlots = new HashMap<>();

    public static Map<Long, Integer> slModificationCount = new HashMap<>();

    public static Map<Long, Boolean> slOrdersExecutionFlags = new HashMap<>();

    public static BigDecimal ltpPriceIncrease = BigDecimal.ZERO;

    public static BigDecimal ltpPriceDecrease = BigDecimal.ZERO;

    public static Map<Long, List<Integer>> orderFeedPushQuantityList = new HashMap<>();

    public static boolean isLatestTrendFinderActive = true;

    public static Map<String, LocalDateTime> slCheckTimer= new HashMap<>();

    public static List<AngelOneInstrumentMasterEntity> angelOneInstruments = new ArrayList<>();

    @Getter
    private static Map<String, String> userDayLimitExceeded = new HashMap<>();


    public static Map<Long, String> activeInstruments = new HashMap<>();

    public static Map<Long, Boolean> slOrderFallBackCallFlag = new HashMap<>();

    public static volatile Boolean isBigCandleProcessStart = false;

    public static Map<String, Boolean> failureAfterConsecutiveSuccess = new HashMap<>();


    public static Set<BigDecimal> trendSet = new LinkedHashSet<>();

    public static Map<Long, OrderRequest> scalpOpenOrders = new ConcurrentHashMap<>();

    public static Map<Long, Boolean> isOrderFeedReceivedMap = new ConcurrentHashMap<>();

    public static Queue<OrderFeedWSMessage> orderFeedQueue = new ConcurrentLinkedQueue<>();

    public static BigDecimal lastLtpTrendValue = BigDecimal.ZERO;

    public static BigDecimal indiaVixStrike = BigDecimal.ZERO;

    public static Map<Long, Boolean> isMonitorFLowStartedForSLOrder = new HashMap<>();

    public static Integer lossRecoveryQuantity = 0;

    public static boolean isWebsocketConnected = false;

    public static boolean isOrderFeedConnected;

    public static Set<OrderMessage> orderFeedSet = new HashSet<>();

    // This SYSTEM_GENERATED_ORDERS will be used to keep track of system generated orders only
    public static Map<Long, OrderRequest> SYSTEM_GENERATED_ORDERS = new HashMap<>();

    public static Optional<CandleEntity> getNiftyBankCandle(LocalDateTime timestamp) {
        logger.debug("Searching Candle for timestamp {}", timestamp);
        return bankNiftyCandels.values().stream()
                .filter(c -> timestamp.isAfter(c.getStartTime()) && timestamp.isBefore(c.getEndTime())).findAny();
    }

    public static Map<String, CandleEntity> getNiftyBankCandle() {
        return bankNiftyCandels;
    }

    public static Map<String, CandleEntity> updateBankNiftyCandelMap(String start, CandleEntity value) {
        logger.debug(" ******[ Date ]:-[ Value ]  {} ,{}", start, value);
        bankNiftyCandels.put(start, value);
        return bankNiftyCandels;
    }

    public static BigDecimal getXPercentOfY(BigDecimal x, BigDecimal y) {
        if (x == null || y == null || x.compareTo(BigDecimal.ZERO) == 0 || y.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return y.divide(new BigDecimal(100), 3, RoundingMode.CEILING).multiply(x);
    }

    public static BigDecimal getPercent(BigDecimal from, BigDecimal to) {
        if (from == null || to == null || from.compareTo(BigDecimal.ZERO) == 0 || to.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal difference = to.subtract(from);
        return difference.divide(from, 3, RoundingMode.CEILING).multiply(new BigDecimal(100));
    }


    public static boolean isBetweenRange(BigDecimal from, BigDecimal to, BigDecimal value) {
        if (from == null || to == null || value == null) {
            return false;
        }
        return value.compareTo(from) >= 0 && value.compareTo(to) <= 0;
    }

    public static Boolean isPlus(BigDecimal from, BigDecimal to) {
        BigDecimal percent = getPercent(from, to);
        return percent.compareTo(BigDecimal.ZERO) == 1;
    }

    public Boolean isDownTrend(BigDecimal from, BigDecimal to) {
        BigDecimal percent = getPercent(from, to);
        BigDecimal trendLimit = new BigDecimal("0.30");
        return percent.compareTo(BigDecimal.ZERO) == -1 && percent.compareTo(trendLimit) == -1;
    }

    public Boolean isUpTrend(BigDecimal from, BigDecimal to) {
        BigDecimal percent = getPercent(from, to);
        BigDecimal trendLimit = new BigDecimal("0.30");
        return percent.compareTo(BigDecimal.ZERO) == 1 && percent.compareTo(trendLimit) == 1;
    }

    public static boolean isAmountValue(String input) {
        if (input == null) {
            return false;
        }
        return Pattern.compile("-?\\d+(\\.\\d+)?").matcher(input).matches();
    }

    public static Map<Long, BigDecimal> getPremiumHighValues() {
        return premiumHighValues;
    }

    public static void setPremiumHighValues(Long premium, BigDecimal highPrice) {
        TradeUtils.premiumHighValues.put(premium, highPrice);
    }

    public static void removeInstrumentFromHighValues(Long instrumentToken) {
        TradeUtils.premiumHighValues.remove(instrumentToken);
    }

    public static Map<Long, BigDecimal> getPremiumLowValues() {
        return premiumLowValues;
    }

    public static void setPremiumLowValues(Long premium, BigDecimal highPrice) {
        TradeUtils.premiumLowValues.put(premium, highPrice);
    }

    public static Optional<CandleEntity> getStockCandle(LocalDateTime timestamp, String stock) {
        logger.debug("Searching Candle for timestamp {} ,Stock {}", timestamp, stock);
        return stockCandles.get(stock).values().stream()
                .filter(c -> timestamp.isAfter(c.getStartTime()) && timestamp.isBefore(c.getEndTime())).findAny();
    }

    public static Map<String, CandleEntity> updateStockCandle(String stock, String start, CandleEntity value) {
        logger.debug(" ******[ Date ]:-[ Value ]  {} ,{}", start, value);
        stockCandles.get(stock).put(start, value);
        return stockCandles.get(stock);
    }

    public static Map<String, CandleEntity> getStockCandle(String stock) {
        logger.debug("Searching Candle for stock {} ", stock);
        return stockCandles.get(stock);
    }

    public Set<Long> getWebsocketInstruments() {
        return instrumentWatchMasterRepo.findAll().stream().map(i -> i.getInstrumentToken())
                .collect(Collectors.toSet());
    }

    public Map<Long, String> getWebsocketALLInstruments() {
        logger.info("getWebsocketALLInstruments called.");
        if (activeInstruments.isEmpty()) {
            activeInstruments = instrumentWatchMasterRepo.findAll().stream().collect(Collectors.toMap(InstrumentWatchEntity::getInstrumentToken, InstrumentWatchEntity::getInstrumentName));
            return activeInstruments;
        }
        return activeInstruments;
    }

    public void setWebsocketInstruments(Set<Long> websocketInstruments) {
        if (!CollectionUtils.isEmpty(websocketInstruments)) {
            Set<InstrumentWatchEntity> entities = websocketInstruments.stream().map(in -> {
                return InstrumentWatchEntity.builder().instrumentToken(in).build();
            }).collect(Collectors.toSet());
            logger.info("Adding Instrument {} in watchlist", entities.size());
            instrumentWatchMasterRepo.saveAll(entities);
        }
    }

    public boolean addWebsocketInstruments(Long websocketInstrument, String instrumentName) throws DuplicateKeyException {
        logger.info("Adding Instrument {} in watchlist", websocketInstrument);
        synchronized (this) {
            Optional<InstrumentWatchEntity> instrumentToken = instrumentWatchMasterRepo.findByInstrumentToken(websocketInstrument);
            if (instrumentToken.isEmpty()) {
                logger.debug("***** Inside empty check for instrument {} ***", websocketInstrument);
                instrumentWatchMasterRepo.save(InstrumentWatchEntity.builder().instrumentToken(websocketInstrument).instrumentName(instrumentName).build());
                activeInstruments.put(websocketInstrument, instrumentName);
                return true;
            }
        }
        return true;
    }

    public void deleteAllWatchInstruments() {
        logger.info("Deleting all Instrument from watchlist");
        instrumentWatchMasterRepo.deleteAll();
    }

    public static Map<Integer, List<Integer>> getTimeFrameSlots() {
        return timeFrameSlots;
    }

    public static void setTimeFrameSlots(Map<Integer, List<Integer>> timeFrameSlots) {
        TradeUtils.timeFrameSlots = timeFrameSlots;
    }

    public static void setUserDayLimitExceeded(Map<String, String> userDayLimitExceeded) {
        TradeUtils.userDayLimitExceeded = userDayLimitExceeded;
    }

    public static boolean isGreaterThan(BigDecimal firstValue, BigDecimal secondValue) {
        return firstValue.compareTo(secondValue) > 0;
    }

    public static boolean isLessThan(BigDecimal firstValue, BigDecimal secondValue) {
        return firstValue.compareTo(secondValue) < 0;
    }


    public static String getLtpRequest(List<StockMaster> allStocks) {
        String allStockLtp = allStocks.stream()
                .map(o -> String.valueOf(o.getInstrumentToken()))
                .collect(Collectors.joining(CharConstant.DASH));
        return allStockLtp;
    }

    public static <T> List<List<T>> splitIntoBatches(List<T> originalList, int batchSize) {
        return IntStream.iterate(0, x -> x < originalList.size(), x -> x + batchSize).mapToObj(x -> originalList.subList(x, Math.min(x + batchSize, originalList.size()))).collect(Collectors.toList());
    }

    public static List<Integer> getClosestStrikePrices(BigDecimal spotPrice) {
        int strikeBasePrice;
        int niftyBankSpotPrice = spotPrice.intValue();
        List<Integer> strikePrices = new ArrayList<>();

        /*- Find Nearest STRIKE PRICE OF SPOT PRICE
         *
         * E.G.  42230 SPOT PRICE
         * REMINDER : 30
         * QUOTIENT : 422
         *
         *
         * E.G.  42200 SPOT PRICE
         * REMINDER : 30
         * QUOTIENT : 422
         *
         */
        int reminder = niftyBankSpotPrice % 100;
        int quotient = niftyBankSpotPrice / 100;

        if (reminder > 50) {
            strikeBasePrice = quotient * 100 + 100;
            int tempPrice = niftyBankSpotPrice == strikeBasePrice ? strikeBasePrice + 100 : strikeBasePrice;
            strikePrices.add(tempPrice);
            strikePrices.add(strikeBasePrice - 100);
        }
        if (reminder <= 50) {
            /*-
             *  THIS CONDITION WILL BE SATISFIED HERE
             */
            strikeBasePrice = quotient * 100; // strikeBasePrice = 42200
            strikePrices.add(strikeBasePrice + 100); // next StrikePrice 42200 + 100 = 42300
            int tempPrice = niftyBankSpotPrice == strikeBasePrice ? strikeBasePrice - 100 : strikeBasePrice;
            strikePrices.add(tempPrice);
        }
        /*-
         *  LIST WILL HAVE
         *  42300,42200
         */
        return strikePrices;
    }

}