package com.trade.algotrade.service.impl;

import com.mongodb.DuplicateKeyException;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.dto.BigCandle;
import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.enums.CandleColor;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.repo.CandleRepository;
import com.trade.algotrade.service.CandleService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CandleServiceImpl implements CandleService {

    private static final Logger logger = LoggerFactory.getLogger(CandleServiceImpl.class);

    @Autowired
    CandleRepository candleRepository;

    @Value("${application.candle.timeframe}")
    Integer candleTimeFrame;

    @Value("${application.stockCandle.timeframe}")
    Integer stockCandle;

    @Autowired
    CommonUtils commonUtils;

    private static List<BigCandle> bigCandle = new ArrayList<>();

    @Override
    public Optional<CandleEntity> findBySymbolLastCandle(String symbol) {
        Optional<CandleEntity> candleOptional = candleRepository.findByStockSymbolOrderByModifiedTime(symbol);
        if (candleOptional.isPresent()) {
            if (DateUtils.getCurrentDateTimeIst().isAfter(candleOptional.get().getStartTime())
                    && DateUtils.getCurrentDateTimeIst().isBefore(candleOptional.get().getEndTime())) {
                return candleOptional;
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public CandleEntity buildCandleBankNiftyCandles(String symbol, BigDecimal value) {
        logger.debug("**** [buildCandleBankNiftyCandles ] Building Nifty Bank Candles {} ", value);
        generateTrendData(value);
        LocalDateTime currentDateTime = DateUtils.getCurrentDateTimeIst();
        Optional<CandleEntity> niftyBankCandleOptional = TradeUtils.getNiftyBankCandle(currentDateTime);
        if (niftyBankCandleOptional.isEmpty()) {
            // This is added to avoid multiple save
            synchronized (this) {
                updateOldCandle();
            }
            currentDateTime = TradeUtils.isFirstCandleFlag ? currentDateTime : getCandleTimeSlot();
            if (!currentDateTime.isAfter(DateUtils.getCurrentDateTimeIst())) {
                String candleStartTime = currentDateTime.format(DateUtils.getCandledatetimeformatter());
                CandleEntity createNewCandle = createNewCandle(value, symbol, candleTimeFrame, currentDateTime);
                createNewCandle.setCandleStart(candleStartTime);
                TradeUtils.updateBankNiftyCandelMap(candleStartTime, createNewCandle);
                TradeUtils.isFirstCandleFlag = true;
                logger.debug("**** Created new candle for symbol {} ****", symbol);
                return createNewCandle;
            } else {
                logger.info("**** [buildCandleBankNiftyCandles ] Waiting to build Candle for time slot {} ",
                        currentDateTime);
                return null;
            }
        } else {
            CandleEntity bankNiftyCandle = niftyBankCandleOptional.get();
            updateCandleData(value, bankNiftyCandle);
            TradeUtils.updateBankNiftyCandelMap(
                    bankNiftyCandle.getStartTime().format(DateUtils.getCandledatetimeformatter()), bankNiftyCandle);
            logger.debug("**** Updated candle for symbol {} ****", bankNiftyCandle.getStockSymbol());
            return bankNiftyCandle;
        }
    }

    private void updateOldCandle() {
        Optional<CandleEntity> lastCandle = TradeUtils.getNiftyBankCandle().values().stream().min((o1, o2) -> o2.getModifiedTime().compareTo(o1.getModifiedTime()));
        if (lastCandle.isPresent()) {
            CandleEntity entity = lastCandle.get();
            entity.setModifiedTime(DateUtils.getCurrentDateTimeIst());
            entity.setCandleBodyPoints(entity.getClose().subtract(entity.getOpen()));
            entity.setCandleTotalPoints(entity.getHigh().subtract(entity.getLow()));
            entity.setCandleComplete(true);
            saveCandle(entity);

        }
    }

    private CandleEntity createNewCandle(BigDecimal ltpValue, String symbol, Integer timeFrame,
                                         LocalDateTime currentDateTime) {
        CandleEntity candle = new CandleEntity();
        candle.setStockSymbol(symbol);
        candle.setStartTime(currentDateTime);
        candle.setOpen(ltpValue);
        candle.setHigh(ltpValue);
        candle.setLow(ltpValue);
        candle.setClose(ltpValue);
        candle.setLtp(ltpValue);
        candle.setEndTime(currentDateTime.plusMinutes(timeFrame));
        candle.setModifiedTime(DateUtils.getCurrentDateTimeIst());
        return candle;
    }

    private void updateCandleData(BigDecimal ltpValue, CandleEntity candle) {
        candle.setClose(ltpValue);
        candle.setLtp(ltpValue);
        if (candle.getHigh() != null) {
            candle.setHigh(0 < ltpValue.compareTo(candle.getHigh()) ? ltpValue : candle.getHigh());
        }
        if (candle.getLow() != null) {
            candle.setLow(ltpValue.compareTo(candle.getLow()) < 0 ? ltpValue : candle.getLow());
        }
        if (candle.getOpen() != null && candle.getClose() != null
                && candle.getOpen().compareTo(candle.getClose()) > 0) {
            candle.setColor(CandleColor.RED);
        }
        if (candle.getOpen() != null && candle.getClose() != null
                && candle.getOpen().compareTo(candle.getClose()) < 0) {
            candle.setColor(CandleColor.GREEN);
        }
        candle.setModifiedTime(DateUtils.getCurrentDateTimeIst());
    }

    @Override
    public void saveCandle(CandleEntity candle) {
        logger.debug(" ******[ CandleServiceImpl ]:-[ saveCandle ] Called  ********");
        if (candle != null) {
            try {
                candleRepository.save(candle);
            } catch (DuplicateKeyException e) {
                logger.error("MongoDB DuplicateKeyException {}", e.getMessage());
            } catch (org.springframework.dao.DuplicateKeyException e) {
                logger.error("org.springframework.dao.DuplicateKeyException {}", e.getMessage());
            }
        }
        logger.debug(" ******[ CandleServiceImpl ]:-[ saveCandle ] Completed  ********");
    }

    @Override
    @Cacheable(value = "candle", key = "#stockSymbol")
    public List<CandleEntity> findBySymbol(String stockSymbol) {
        return candleRepository.findByStockSymbolIgnoreCase(stockSymbol);
    }

    @Override
    public CandleEntity buildCandles(String symbol, BigDecimal value) {
        logger.debug("**** [buildCandles ] Building Stock {}  Bank Candles {} ", symbol, value);
        LocalDateTime currentDateTime = getCandleTimeSlot();
        if (currentDateTime.isEqual(DateUtils.getCurrentDateTimeIst())) {
            logger.debug("**** [buildCandles ] Building Candle for time slot {} ", currentDateTime);
            Optional<CandleEntity> candleOptional = TradeUtils.getStockCandle(currentDateTime, symbol);
            if (candleOptional.isEmpty()) {
                updateOldStockCandle(symbol);
                String candleStartTime = currentDateTime.format(DateUtils.getCandledatetimeformatter());
                CandleEntity createNewCandle = createNewCandle(value, symbol, stockCandle, currentDateTime);
                createNewCandle.setCandleStart(candleStartTime);
                TradeUtils.updateStockCandle(symbol, candleStartTime, createNewCandle);
                return createNewCandle;
            } else {
                CandleEntity bankNiftyCandle = candleOptional.get();
                updateCandleData(value, bankNiftyCandle);
                TradeUtils.updateStockCandle(symbol,
                        bankNiftyCandle.getStartTime().format(DateUtils.getCandledatetimeformatter()), bankNiftyCandle);
                return bankNiftyCandle;
            }
        } else {
            logger.debug("**** [buildCandles ] Waiting to build Candle for time slot {} ", currentDateTime);
            return null;
        }
    }

    private void updateOldStockCandle(String stock) {
        logger.debug("**** [updateOldStockCandle ] Updating stock Candle {}", stock);
        Optional<CandleEntity> lastCandle = TradeUtils.getStockCandle(stock).values().stream().min((o1, o2) -> o2.getModifiedTime().compareTo(o1.getModifiedTime()));
        if (lastCandle.isPresent()) {
            CandleEntity entity = lastCandle.get();
            entity.setModifiedTime(DateUtils.getCurrentDateTimeIst());
            entity.setCandleComplete(true);
        }
        logger.debug("**** [updateOldStockCandle ] Updating stock Candle {} Completed", stock);
    }

    // @Cacheable(value = "candle", key = "#stockSymbol")
    // TODO Need to fix this conflict
    public List<CandleEntity> findCandlesBySymbolsDescending(String symbol) {
        return candleRepository.findByStockSymbolIgnoreCaseOrderByStartTimeDesc(symbol);
    }

    public void generateTimeSlotUsingTimeFrame() {
        Map<Integer, List<Integer>> timeSlots = new HashMap<>();
        int hour = 9;
        for (int i = 0; i < 7; i++) {
            List<Integer> minutesList = new ArrayList<>();
            Integer minutes = candleTimeFrame;
            for (int j = 0; j < (60 / candleTimeFrame); j++) {
                minutesList.add(minutes * j);
            }
            timeSlots.put((hour + i), minutesList);
        }
        TradeUtils.setTimeFrameSlots(timeSlots);
        logger.debug("**** [generateTimeSlotUsingTimeFrame ] Generating time slots based on candle time frame := {} ", candleTimeFrame);
    }

    private LocalDateTime getCandleTimeSlot() {
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalTime time = zonedDateTime.toLocalTime();
        String t = time.format(DateTimeFormatter.ofPattern("HH:mm"));
        String[] split = t.split(":");

        Map<Integer, List<Integer>> timeSlots = !TradeUtils.getTimeFrameSlots().isEmpty() ? TradeUtils.getTimeFrameSlots() : null;
        List<Integer> currentSlot = Objects.nonNull(timeSlots) ? timeSlots.get(Integer.parseInt(split[0])) : null;
        LocalDateTime localDateTime;
        if (Objects.isNull(currentSlot)) {
            localDateTime = DateUtils.getDateTime(LocalDate.now(), commonUtils.getConfigValue(ConfigConstants.CANDLE_DATA_CAPTURE_START_TIME));

        } else {
            Optional<Integer> findFirst = timeSlots.get(9).stream().filter(minutes -> minutes >= Integer.parseInt(split[1])).findFirst();
            localDateTime = findFirst.map(integer -> LocalDate.now().atTime(Integer.parseInt(split[0]), integer)).orElseGet(() -> LocalDate.now().atTime(Integer.parseInt(split[0]) + 1, 0));
        }
        return localDateTime;
    }

    @Override
    public BigDecimal latestTrendFinder() {
        if (!DateUtils.isBeforeTime(commonUtils.getConfigValue(ConfigConstants.BIG_CANDLE_TRADING_START_TIME))) {
            if (TradeUtils.isLatestTrendFinderActive) {
                logger.debug("Trend is Active now");
                ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
                // TODO  Get all configs and filter required from it using java.
                String bigCandleTimeFrame = commonUtils.getConfigValue(ConfigConstants.BIG_CANDLE_TREND_FINDER_TIME_FRAME);
                if (bigCandleTimeFrame == null) {
                    throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
                }
                LocalDateTime localDateTime1 = localDateTime.minusMinutes(Long.parseLong(bigCandleTimeFrame));
                List<BigCandle> candleList = bigCandle.stream().filter(candle -> candle.getDateTime().isAfter(localDateTime1) && candle.getDateTime().isBefore(localDateTime)).collect(Collectors.toList());
                Optional<BigDecimal> maxLtp = candleList.stream().map(BigCandle::getLtp).max(Comparator.naturalOrder());
                Optional<BigDecimal> minLtp = candleList.stream().map(BigCandle::getLtp).min(Comparator.naturalOrder());
                if (maxLtp.isPresent()) {
                    BigDecimal trendValue = maxLtp.get().subtract(minLtp.get());
                    logger.info("********* BANK NIFTY FROM {} TO {} the current Trend value :{}", localDateTime1, localDateTime, trendValue);
                    String avgTrendPointStartConfigValue = commonUtils.getConfigValue(ConfigConstants.AVG_TREND_POINTS_START_FROM);
                    addTrendDifference(avgTrendPointStartConfigValue, trendValue);
                    return trendValue;
                }
            }
        }
        return null;
    }

    private static void addTrendDifference(String avgTrendPointStartConfigValue, BigDecimal trendValue) {
        BigDecimal avgTrendPointStart = Objects.nonNull(avgTrendPointStartConfigValue) ? new BigDecimal(avgTrendPointStartConfigValue) : new BigDecimal(45);
        if (TradeUtils.lastLtpTrendValue.compareTo(BigDecimal.ZERO) == 0) {
            TradeUtils.lastLtpTrendValue = avgTrendPointStart;
        }
        if (avgTrendPointStart.compareTo(trendValue) < 0) {
            if (trendValue.subtract(TradeUtils.lastLtpTrendValue).compareTo(BigDecimal.ZERO) != 0) {
                TradeUtils.trendSet.add(trendValue.subtract(TradeUtils.lastLtpTrendValue).abs());
            }
            TradeUtils.lastLtpTrendValue = trendValue;
        }
    }

    public void clearTrendData() {
        bigCandle.clear();;
    }

    @Override
    public void generateTrendData(BigDecimal value) {
        if (!DateUtils.isBeforeTime(commonUtils.getConfigValue(ConfigConstants.BIG_CANDLE_TRADING_START_TIME))) {
            ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
            BigCandle candle = BigCandle.builder().ltp(value).dateTime(localDateTime).build();
            bigCandle.add(candle);
        }
    }
}