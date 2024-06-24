package com.trade.algotrade.service.impl.equity;

import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;
import com.trade.algotrade.client.kotak.dto.OhlcSuccess;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.client.kotak.request.TargetDetails;
import com.trade.algotrade.client.kotak.response.LtpResponse;
import com.trade.algotrade.client.kotak.response.MarketOhlcResponse;
import com.trade.algotrade.client.nse.enums.ExchangeEquityFileNames;
import com.trade.algotrade.constants.CharConstant;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.enitiy.equity.NewsSpikeStock;
import com.trade.algotrade.enitiy.equity.StockDayCandle;
import com.trade.algotrade.enitiy.equity.StockMaster;
import com.trade.algotrade.enitiy.equity.TopGainerLooserEntity;
import com.trade.algotrade.enums.MoversType;
import com.trade.algotrade.enums.Strategy;
import com.trade.algotrade.enums.TransactionType;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.repo.equity.NewsSpikeStoksRepository;
import com.trade.algotrade.repo.equity.StockMasterRepository;
import com.trade.algotrade.repo.equity.TopGainerLoosersRepository;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.service.EquityStrategyBuilderService;
import com.trade.algotrade.service.equity.StockMasterService;
import com.trade.algotrade.service.impl.PeriodicExecutorService;
import com.trade.algotrade.sort.SortUtils;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockMasterServiceImpl implements StockMasterService {

    private final Logger logger = LoggerFactory.getLogger(StockMasterServiceImpl.class);

    @Autowired
    StockMasterRepository stockMasterRepository;

    @Autowired
    TopGainerLoosersRepository topGainerLoosersRepository;


    @Autowired
    NewsSpikeStoksRepository newsSpikeStoksRepository;


    AtomicInteger topGainerOrderCount = new AtomicInteger(0);

    AtomicInteger topLooserOrderCount = new AtomicInteger(0);

    @Autowired
    KotakClient kotakClient;


    @Autowired
    CommonUtils commonUtils;

    @Autowired
    TradeUtils tradeUtils;


    @Autowired
    PeriodicExecutorService periodicExecutorService;


    @Autowired
    EquityStrategyBuilderService equityStrategyBuilderService;

    @Override
    public List<StockMaster> getAllStocks() {
        return stockMasterRepository.findAll();
    }

    @Override
    public StockMaster createEquity(String stockName, StockMaster stock) {
        logger.info(" ****** [getStockByName] Called ******** Stock {} , Stock Request {}", stockName, stock);
        Optional<StockMaster> existingStockOptional = stockMasterRepository.findByStockName(stockName);
        if (existingStockOptional.isEmpty()) {
            throw new AlgotradeException(ErrorCodeConstants.STOCK_MASTER_NOT_FOUND);
        }
        StockMaster stockMaster = existingStockOptional.get();
        stockMaster.setInstrumentToken(stock.getInstrumentToken());
        stockMaster.setLtp(stock.getLtp());
        stockMaster.setSectorName(stock.getSectorName());
        stockMaster.setStockSymbol(stock.getStockSymbol());
        return stockMasterRepository.save(stockMaster);
    }

    @Override
    public StockMaster getStockByName(String stockName) {
        logger.info(" ****** [getStockByName] Called ******** Stock {}", stockName);
        Optional<StockMaster> existingStockOptional = stockMasterRepository.findByStockName(stockName);
        if (existingStockOptional.isEmpty()) {
            throw new AlgotradeException(ErrorCodeConstants.STOCK_MASTER_NOT_FOUND);
        }
        return existingStockOptional.get();
    }

    @Override
    public void deleteStock(String stockName) {
        logger.info(" ****** [deleteStock] Called ******** Stock {}", stockName);
        stockMasterRepository.deleteByStockName(stockName);
    }

    @Override
    public void updateAll(List<StockMaster> allStocks) {
        logger.info(" ****** [updateAll] Called ******** Total {} Updating ", allStocks.size());
        if (!CollectionUtils.isEmpty(allStocks)) {
            stockMasterRepository.saveAll(allStocks);
        }
    }

    @Override
    public void addStock(StockMaster stockMaster) {
        logger.info(" ****** [addStock] Called ******** StockMaster {}", stockMaster);
        if (stockMaster != null) {
            Optional<StockMaster> existingStockSymbol = stockMasterRepository
                    .findByStockSymbol(stockMaster.getStockSymbol());
            if (existingStockSymbol.isPresent()) {
                throw new AlgotradeException(ErrorCodeConstants.DUPLICATE_STOCK_MASTER);
            }
            stockMasterRepository.save(stockMaster);
        }
    }

    @Override
    public List<TopGainerLooserEntity> findTopLoosers() {
        logger.info(" ****** [getTopTenLooser] Called ********");
        List<StockMaster> findMostActiveStocks = findMostActiveStocks(SortUtils.percentageChangeAscComparator());
        if (!CollectionUtils.isEmpty(findMostActiveStocks)) {
            return findMostActiveStocks.stream().map(tl -> {
                TopGainerLooserEntity topLooser = mapStockMasterToTopGainerLooserEntity(tl);
                topLooser.setType(MoversType.LOOSER);
                return topLooser;
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<StockMaster> findMostActiveStocks(Comparator<StockMaster> percentageChangeComparator) {
        List<StockMaster> allStocks = getAllStocks();
        if (CollectionUtils.isEmpty(allStocks)) {
            throw new AlgotradeException(ErrorCodeConstants.STOCK_MASTER_NOT_FOUND);
        }
        List<StockMaster> topLooser = allStocks.stream().sorted(percentageChangeComparator).limit(5)
                .collect(Collectors.toList());
        return topLooser;
    }

    @Override
    public List<TopGainerLooserEntity> findTopGainers() {
        logger.info(" ****** [getTopTenGainer] Called ********");
        List<StockMaster> topGainers = findMostActiveStocks(SortUtils.percentageChangeAscComparator().reversed());
        if (!CollectionUtils.isEmpty(topGainers)) {
            return topGainers.stream().map(tg -> {
                TopGainerLooserEntity topLooser = mapStockMasterToTopGainerLooserEntity(tg);
                topLooser.setType(MoversType.GAINER);
                return topLooser;
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private TopGainerLooserEntity mapStockMasterToTopGainerLooserEntity(StockMaster tl) {
        TopGainerLooserEntity topLooser = new TopGainerLooserEntity();
        BeanUtils.copyProperties(tl, topLooser);
        topLooser.setCreatedAt(LocalDateTime.now());
        topLooser.setLastUpdatedAt(LocalDateTime.now());
        return topLooser;
    }

    @Override
    public List<TopGainerLooserEntity> findAndSaveTopMovers() {
        logger.info(" ****** [findAndSaveTopMovers] Called ********");
        List<TopGainerLooserEntity> topGainers = findTopGainers();
        List<TopGainerLooserEntity> topLosers = findTopLoosers();
        if (CollectionUtils.isEmpty(topGainers)) {
            topGainers = topLosers;
        } else {
            topGainers.addAll(topLosers);
        }
        if (!CollectionUtils.isEmpty(topGainers)) {
            topGainerLoosersRepository.deleteAll();
            topGainerLoosersRepository.saveAll(topGainers);
        }
        logger.info(" ****** [getTopTenMoves] Completed ********");
        return topGainers;
    }

    @Override
    public void updateDayClose() {
        logger.info(" ****** [updateDayClose] Called ********");
        List<StockMaster> allStocks = getAllStocks();
        if (CollectionUtils.isEmpty(allStocks)) {
            return;
        }
        Integer batchSize = Integer.valueOf(commonUtils.getConfigValue(ConfigConstants.EQUITY_LTP_BATCH_SIZE));
        List<List<StockMaster>> stockBatches = TradeUtils.splitIntoBatches(allStocks, batchSize);
        List<CompletableFuture<Void>> threads;
        if (!CollectionUtils.isEmpty(stockBatches)) {
            threads = stockBatches.stream().map(batch -> {
                return CompletableFuture.supplyAsync(() -> {
                    return updateWeeklyStockHistory(TradeUtils.getLtpRequest(batch), batch);
                }).thenAccept(stocks -> {
                    updateAll(stocks);
                });
            }).collect(Collectors.toList());
            CompletableFuture.allOf(threads.toArray(new CompletableFuture[0]));
        }
        logger.info(" ****** [updateDayClose] Completed ********");

    }

    private List<StockMaster> updateWeeklyStockHistory(String allStockLtp, List<StockMaster> dailyClosedUpdatedStocks) {
        logger.info(" ****** [updateWeeklyStockHistory] Called ********");
        MarketOhlcResponse marketOhlc = kotakClient.getMarketOhlc(allStockLtp);
        if (marketOhlc != null) {
            List<OhlcSuccess> ohlcs = marketOhlc.getSuccess();
            if (!CollectionUtils.isEmpty(ohlcs)) {
                Map<Long, OhlcSuccess> ohlcMap = ohlcs.stream()
                        .collect(Collectors.toMap(OhlcSuccess::getInstrumentToken, Function.identity()));
                return dailyClosedUpdatedStocks.stream().map(stock -> {
                    OhlcSuccess ohlcSuccess = ohlcMap.get(stock.getInstrumentToken());
                    if (ohlcSuccess != null) {
                        updateStockPrices(stock, ohlcSuccess);
                    }
                    return stock;
                }).collect(Collectors.toList());
            }
        }
        logger.info(" ****** [updateWeeklyStockHistory] Completed ********");
        return new ArrayList<>();
    }

    private void updateStockPrices(StockMaster stock, OhlcSuccess ohlcSuccess) {
        BigDecimal lastPrice = ohlcSuccess.getClose();
        stock.setChangePercent(TradeUtils.getPercent(stock.getPreviousClose(), stock.getLtp()));
        stock.setPreviousClose(lastPrice);
        stock.setLastUpdatedAt(LocalDateTime.now());
        stock.setAllTimeHigh(stock.getAllTimeHigh() != null ? stock.getAllTimeHigh().compareTo(lastPrice) < 1 ? lastPrice : stock.getAllTimeHigh() : lastPrice);
        stock.setChangePercentRelativeAllTimeHigh(stock.getAllTimeHigh() != null ? TradeUtils.getPercent(stock.getAllTimeHigh(), lastPrice) : BigDecimal.ZERO);
        List<StockDayCandle> weekHistory = stock.getWeekHistory();
        StockDayCandle todayCandle = StockDayCandle.builder().build();
        todayCandle.setDate(LocalDate.now());
        todayCandle.setHigh(ohlcSuccess.getHigh()); // Need to get from OHLC
        todayCandle.setLow(ohlcSuccess.getLow());
        todayCandle.setOpen(ohlcSuccess.getOpen());
        todayCandle.setClose(ohlcSuccess.getClose());
        todayCandle.setPreviousClose(stock.getPreviousClose());
        todayCandle.setChangePercent(TradeUtils.getPercent(stock.getPreviousClose(), stock.getLtp()));
        List<StockDayCandle> updatedWeekHistory;
        if (!CollectionUtils.isEmpty(weekHistory)) {
            updatedWeekHistory = weekHistory.stream().filter(wh -> wh.getDate().isAfter(LocalDate.now().minusDays(Long.valueOf(commonUtils.getConfigValue(ConfigConstants.STOCK_PRICE_HISTORY_DAYS))))).collect(Collectors.toList());
            updatedWeekHistory.add(todayCandle);
            stock.setWeekHistory(updatedWeekHistory);
        } else {
            updatedWeekHistory = new ArrayList<>();
            updatedWeekHistory.add(todayCandle);
            stock.setWeekHistory(updatedWeekHistory);
        }
    }

    @Override
    public void updateDayOpen() {
        logger.info(" ****** [updateDayOpen] Called ********");
        List<StockMaster> allStocks = getAllStocks();
        if (CollectionUtils.isEmpty(allStocks)) {
            return;
        }
        String allStockLtp = TradeUtils.getLtpRequest(allStocks);
        LtpResponse ltpResponses = kotakClient.getLtp(allStockLtp);
        if (ltpResponses == null) {
            return;
        }

        List<LtpSuccess> lastPrices = ltpResponses.getSuccess();
        if (!CollectionUtils.isEmpty(lastPrices)) {
            Map<Long, LtpSuccess> ltpMap = lastPrices.stream()
                    .collect(Collectors.toMap(LtpSuccess::getInstrumentToken, Function.identity()));
            List<StockMaster> dayOpenUpdatedStocks = allStocks.stream().map(al -> {
                LtpSuccess ltp = ltpMap.get(al.getInstrumentToken());
                if (ltp != null) {
                    BigDecimal lastPrice = ltp.getLastPrice();
                    BigDecimal yesterDayClose = al.getPreviousClose() != null ? al.getPreviousClose() : lastPrice;
                    al.setChangePercent(TradeUtils.getPercent(yesterDayClose, lastPrice));
                    al.setLtp(lastPrice);
                    al.setLastUpdatedAt(LocalDateTime.now());
                    return al;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(dayOpenUpdatedStocks)) {
                logger.info(" ****** [updateDayOpen] {} Total Stocks {} Will be updated ********", dayOpenUpdatedStocks.size());
                updateAll(dayOpenUpdatedStocks);
            }
            logger.info(" ****** [updateDayOpen] Completed ********");
        }
    }

    @Override
    public List<TopGainerLooserEntity> getTopTenMoves() {
        List<TopGainerLooserEntity> topMovers = topGainerLoosersRepository.findAll();
        if (!CollectionUtils.isEmpty(topMovers)) {
            return topMovers;
        }
        return new ArrayList<>();
    }

    @Override
    public List<TopGainerLooserEntity> getTopTenGainer() {
        return topGainerLoosersRepository.findByType(MoversType.GAINER);
    }

    @Override
    public List<TopGainerLooserEntity> getTopTenLooser() {
        return topGainerLoosersRepository.findByType(MoversType.LOOSER);
    }


    @Override
    public void findNewsSpikeStockAndSave() {
        logger.info(" ****** [findNewsSpikeStock] Called ********");
        List<StockMaster> allStocks = getAllStocks();
        if (CollectionUtils.isEmpty(allStocks)) {
            return;
        }
        newsSpikeStoksRepository.deleteAll();
        Integer batchSize = Integer.valueOf(commonUtils.getConfigValue(ConfigConstants.EQUITY_LTP_BATCH_SIZE));
        List<List<StockMaster>> stockBatches = TradeUtils.splitIntoBatches(allStocks, batchSize);
        List<CompletableFuture<List<StockMaster>>> eligibleStocksThread;
        if (!CollectionUtils.isEmpty(stockBatches)) {
            eligibleStocksThread = stockBatches.stream().map(batch -> {
                return CompletableFuture.supplyAsync(() -> {
                    return getLatestPriceForStocks(batch);
                }).thenApplyAsync(ltpResponse -> {
                    List<StockMaster> eligibleStocks = new ArrayList<>();
                    if (ltpResponse != null) {
                        List<LtpSuccess> lastPrices = ltpResponse.getSuccess();
                        return getEligibleStocks(batch, eligibleStocks, lastPrices);
                    }
                    return eligibleStocks;
                }).thenApplyAsync(stocks -> {
                    return saveNewsSpikeStocksToDB(stocks);
                });
            }).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(eligibleStocksThread)) {
                CompletableFuture.allOf(eligibleStocksThread.toArray(new CompletableFuture[0])).join();
            }
        } else {
            eligibleStocksThread = null;
        }

        if (!CollectionUtils.isEmpty(eligibleStocksThread)) {
            CompletableFuture<?>[] futuresArray = eligibleStocksThread.toArray(new CompletableFuture<?>[0]);
            CompletableFuture<List<List<StockMaster>>> allFeature = CompletableFuture.allOf(futuresArray)
                    .thenApply(v -> eligibleStocksThread.stream().map(CompletableFuture::join).collect(Collectors.toList()));
            final List<List<StockMaster>> results = allFeature.join();
            String maxStocksNewsSpikeTrade = commonUtils.getConfigValue(ConfigConstants.NEW_SPIKE_MAX_STOCKS_PER_DAY);
            if (maxStocksNewsSpikeTrade == null) {
                throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
            }
            List<StockMaster> eligibleStockList = results.stream()
                    .flatMap(Collection::stream)
                    .sorted(SortUtils.percentageChangeDescComparator())
                    .collect(Collectors.toList());
            logger.info("****************************************   TOP STOCK   ****************************************");
            eligibleStockList.stream().forEach(nss -> {
                logger.info("NEWS SPIKE STOCK : {}( {} ), Change Percent : {}, Previous Close : {} Today's Price : {},", nss.getStockSymbol(), nss.getStockName(), nss.getChangePercent(), nss.getPreviousClose(), nss.getLtp());
            });
            logger.info("****************************************   TOP STOCK   ****************************************");
            eligibleStockList = eligibleStockList.stream().limit(Integer.valueOf(maxStocksNewsSpikeTrade)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(eligibleStockList)) {
                logger.info("NEWS SPIKE : - PROCESSING ORDERS for {} Stocks", eligibleStockList.size());
                equityStrategyBuilderService.buildEquityStrategy(eligibleStockList, Strategy.EQUITY_NEWS_SPIKE);
            } else {
                logger.info(" ****** No eligible stocks found for NEWS SPIKE ********");
            }
        }
        logger.info(" ****** [updateDayClose] Completed ********");
    }

    private List<StockMaster> saveNewsSpikeStocksToDB(List<StockMaster> eligibleStocks) {

        if (!CollectionUtils.isEmpty(eligibleStocks)) {
            List<NewsSpikeStock> newsSpikeStocks = new ArrayList<>();
            for (StockMaster nss : eligibleStocks) {
                NewsSpikeStock newsSpikeStock = new NewsSpikeStock();
                newsSpikeStock.setStockName(nss.getStockName());
                newsSpikeStock.setChangePercent(nss.getChangePercent());
                newsSpikeStock.setPreviousClose(nss.getPreviousClose());
                newsSpikeStock.setTradeDate(DateUtils.getCurrentDateTimeIst());
                newsSpikeStock.setInstrumentToken(nss.getInstrumentToken());
                newsSpikeStock.setSegment(nss.getSegment());
                newsSpikeStock.setTradePrice(nss.getLtp());
                newsSpikeStock.setCreateAt(DateUtils.getCurrentDateTimeIst());
                newsSpikeStocks.add(newsSpikeStock);
                logger.info("Stock : {}( {} ), Change Percent : {}, Yesterday's Price : {} Today's Price : {},", nss.getStockSymbol(), nss.getStockName(), nss.getChangePercent(), nss.getPreviousClose(), nss.getLtp());
            }
            if (!CollectionUtils.isEmpty(newsSpikeStocks)) {
                logger.info(" ****** [saveNewsSpikeStocksToDB] Saving {}  Eligible Stocks ********", newsSpikeStocks.size());
                newsSpikeStoksRepository.saveAll(newsSpikeStocks);
                logger.info(" ****** [saveNewsSpikeStocksToDB] Saved {}  Eligible Stocks  ********", newsSpikeStocks.size());
            }
        }
        return eligibleStocks;
    }

    private List<StockMaster> getEligibleStocks(List<StockMaster> allStocks, List<StockMaster> eligibleStocks, List<LtpSuccess> lastPrices) {
        logger.info(" ****** [getEligibleStocks]Getting Eligible Stocks for {} Stocks ********", allStocks.size());
        if (!CollectionUtils.isEmpty(lastPrices)) {
            String newSpikeChangePercent = commonUtils.getConfigValue(ConfigConstants.NEW_SPIKE_CHANGE_PERCENT);
            if (newSpikeChangePercent == null) {
                throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
            }
            String newSpikeStocksCount = commonUtils.getConfigValue(ConfigConstants.NEW_SPIKE_STOCKS_COUNT);
            if (newSpikeStocksCount == null) {
                throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
            }
            Map<Long, LtpSuccess> ltpMap = lastPrices.stream()
                    .collect(Collectors.toMap(LtpSuccess::getInstrumentToken, Function.identity()));
            eligibleStocks = allStocks.stream().map(as -> {
                LtpSuccess ltpSuccess = ltpMap.get(as.getInstrumentToken());
                BigDecimal lastPrice = ltpSuccess.getLastPrice();
                BigDecimal yesterDayClose = as.getPreviousClose() != null
                        ? as.getPreviousClose()
                        : lastPrice;
                as.setChangePercent(TradeUtils.getPercent(yesterDayClose, lastPrice));
                as.setLtp(lastPrice);
                as.setLastUpdatedAt(LocalDateTime.now());
                return as;
            }).filter(as -> as.getChangePercent().compareTo(new BigDecimal(newSpikeChangePercent)) > 0).sorted(SortUtils.percentageChangeAscComparator().reversed()).limit(Long.valueOf(newSpikeStocksCount)).collect(Collectors.toList());

        }
        logger.info(" ****** [getEligibleStocks]Got Eligible Stocks for {} Stocks ********", allStocks.size());
        return eligibleStocks;
    }

    private LtpResponse getLatestPriceForStocks(List<StockMaster> allStocks) {
        logger.info(" ****** [getLatestPriceForStocks]Getting LTP for {} Stocks ********", allStocks.size());
        String allStockLtp = TradeUtils.getLtpRequest(allStocks);
        logger.info("LTP Input String {} ", allStockLtp);
        LtpResponse ltpResponses = kotakClient.getLtp(allStockLtp);
        if (ltpResponses == null) {
            return null;
        }
        logger.info(" ****** [getLatestPriceForStocks]Got LTP for {} Stocks ********", allStocks.size());
        return ltpResponses;
    }


    @Override
    public void updateNewsSpikeStockGain() {
        LocalDate yesterDay = getLastTradingSessionDate();
        List<NewsSpikeStock> stocks = newsSpikeStoksRepository.findByTradeDateGreaterThanEqual(yesterDay.atStartOfDay());
        if (!CollectionUtils.isEmpty(stocks)) {
            String allStockLtp = stocks.stream()
                    .map(o -> String.valueOf(o.getInstrumentToken()))
                    .collect(Collectors.joining(CharConstant.DASH));
            MarketOhlcResponse marketOhlc = kotakClient.getMarketOhlc(allStockLtp);

            if (CollectionUtils.isEmpty(marketOhlc.getSuccess())) {
                return;
            }
            Map<Long, OhlcSuccess> ltpMap = marketOhlc.getSuccess().stream()
                    .collect(Collectors.toMap(OhlcSuccess::getInstrumentToken, Function.identity()));
            stocks.forEach(nss -> {
                OhlcSuccess ohlcSuccess = ltpMap.get(nss.getInstrumentToken());
                nss.setUpdateAt(DateUtils.getCurrentDateTimeIst());
                nss.setOrderGainPercent(TradeUtils.getPercent(nss.getTradePrice(), ohlcSuccess.getHigh()));
                nss.setSquareOffDate(DateUtils.getCurrentDateTimeIst());
            });
            newsSpikeStoksRepository.saveAll(stocks);
        }
    }

    private LocalDate getLastTradingSessionDate() {
        LocalDateTime marketOpenDateTime = DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME));
        //TODO Handle Weekend condition or Holiday condition here
        LocalDate yesterDay = marketOpenDateTime.toLocalDate().minusDays(1);
        boolean todayNseHoliday = commonUtils.isTodayNseHoliday(yesterDay);
        while (todayNseHoliday) {
            yesterDay = yesterDay.minusDays(1);
            todayNseHoliday = commonUtils.isTodayNseHoliday(yesterDay);
        }
        return yesterDay;
    }

    @Override
    public void readNseStockCsv(String fileNames, Integer indexStockCount) {
        logger.info("******* [InstrumentServiceImpl][readNseStockCsv] Called");
        Reader targetReader = null;
        CSVParser csvParser = null;
        List<StockMaster> stocks = new ArrayList<>();
        ExchangeEquityFileNames exchangeEquityFileNames = ExchangeEquityFileNames.fromValue(fileNames);
        if (exchangeEquityFileNames == null) {
            throw new AlgotradeException(ErrorCodeConstants.INVALID_ENUM_VALUES);
        }
        try {
            String prefix = "ind_";
            String suffix = "list.csv";
            String fileName = exchangeEquityFileNames.strValue().toLowerCase().concat(String.valueOf(indexStockCount)).concat(suffix);
            File file = new ClassPathResource("nsestocks/".concat(prefix).concat(fileName)).getFile();
            targetReader = new InputStreamReader(new FileInputStream(file));
            csvParser = new CSVParser(targetReader,
                    CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim().withDelimiter(','));
            for (CSVRecord record : csvParser) {
                processRecord(indexStockCount, stocks, exchangeEquityFileNames, record);
            }
        } catch (IOException e) {
            logger.error("[InstrumentServiceImpl][readNseStockCsv] Exception Occuured {}", e.getMessage());
        } finally {
            try {
                if (targetReader != null) {
                    targetReader.close();
                }
                if (!Objects.requireNonNull(csvParser).isClosed()) {
                    csvParser.close();
                }
            } catch (IOException ex) {
                logger.error("[InstrumentServiceImpl][readNseStockCsv] Exception Occurred {}", ex.getMessage());
            }
        }
        if (!CollectionUtils.isEmpty(stocks)) {
            List<StockMaster> existingStocks = getAllStocks();
            List<StockMaster> newStocks = stocks.stream().map(newStock -> {
                Optional<StockMaster> first = existingStocks.stream().filter(es -> es.getStockSymbol().equalsIgnoreCase(newStock.getStockSymbol())).findFirst();
                if (first.isPresent()) {
                    StockMaster stockMaster = first.get();
                    stockMaster.setNifty50Stock(newStock.isNifty50Stock() ? newStock.isNifty50Stock() : stockMaster.isNifty50Stock());
                    stockMaster.setNifty100Stock(newStock.isNifty100Stock() ? newStock.isNifty100Stock() : stockMaster.isNifty100Stock());
                    stockMaster.setNifty500Stock(newStock.isNifty500Stock() ? newStock.isNifty500Stock() : stockMaster.isNifty500Stock());
                    stockMaster.setNiftyMidcap100Stock(newStock.isNiftyMidcap100Stock() ? newStock.isNiftyMidcap100Stock() : stockMaster.isNiftyMidcap100Stock());
                    stockMaster.setNiftySmallcap100Stock(newStock.isNiftySmallcap100Stock() ? newStock.isNiftySmallcap100Stock() : stockMaster.isNiftySmallcap100Stock());
                    stockMaster.setLastUpdatedAt(DateUtils.getCurrentDateTimeIst());
                    return stockMaster;
                }
                return newStock;
            }).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(newStocks)) {
                updateAll(newStocks);
            }
        }
        logger.info("******* [InstrumentServiceImpl][readNseStockCsv] Completed");
    }

    private static void processRecord(Integer indexStockCount, List<StockMaster> stocks, ExchangeEquityFileNames exchangeEquityFileNames, CSVRecord record) {
        StockMaster stockMaster = new StockMaster();
        stockMaster.setStockName(record.get("Company Name"));

        if (ExchangeEquityFileNames.NIFTY == exchangeEquityFileNames && indexStockCount == 50) {
            stockMaster.setNifty50Stock(true);
        }
        if (ExchangeEquityFileNames.NIFTY == exchangeEquityFileNames && indexStockCount == 100) {
            stockMaster.setNifty100Stock(true);
        }
        if (ExchangeEquityFileNames.NIFTY == exchangeEquityFileNames && indexStockCount == 500) {
            stockMaster.setNifty500Stock(true);
        }
        if (ExchangeEquityFileNames.NIFTY_MID_CAP == exchangeEquityFileNames && indexStockCount == 100) {
            stockMaster.setNiftyMidcap100Stock(true);
        }
        if (ExchangeEquityFileNames.NIFTY_SMALL_CAP == exchangeEquityFileNames && indexStockCount == 100) {
            stockMaster.setNiftySmallcap100Stock(true);
        }
        stockMaster.setSectorName(record.get("Industry"));
        stockMaster.setStockSymbol(record.get("Symbol"));
        stockMaster.setIsinCode(record.get("ISIN Code"));
        stockMaster.setSegment(record.get("Series"));
        stocks.add(stockMaster);
    }

    private List<StockMaster> findNewsSpikeStrategyStocks(List<StockMaster> newsSpikeStock) {
        String midCap100NewsSpikeLimitPerDay = commonUtils.getConfigValue(ConfigConstants.MID_CAP_100_NEWS_SPIKE_STOCK_LIMIT);
        if (midCap100NewsSpikeLimitPerDay == null) {
            throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
        }
        List<StockMaster> midCap100Stocks = newsSpikeStock.stream().filter(newsStock -> newsStock.isNiftyMidcap100Stock()).limit(Integer.valueOf(midCap100NewsSpikeLimitPerDay)).collect(Collectors.toList());
        String smallCap100NewsSpikeLimitPerDay = commonUtils.getConfigValue(ConfigConstants.SMALL_CAP_100_NEWS_SPIKE_STOCK_LIMIT);
        if (smallCap100NewsSpikeLimitPerDay == null) {
            throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
        }
        List<StockMaster> smallCap100Stocks = newsSpikeStock.stream().filter(newsStock -> newsStock.isNiftySmallcap100Stock()).limit(Integer.valueOf(smallCap100NewsSpikeLimitPerDay)).collect(Collectors.toList());

        String nifty500NewsSpikeLimitPerDay = commonUtils.getConfigValue(ConfigConstants.NIFTY_500_NEWS_SPIKE_STOCK_LIMIT);
        if (nifty500NewsSpikeLimitPerDay == null) {
            throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
        }
        List<StockMaster> nifty500Stocks = newsSpikeStock.stream().filter(newsStock -> newsStock.isNifty500Stock()).limit(Integer.valueOf(nifty500NewsSpikeLimitPerDay)).collect(Collectors.toList());

        String nifty50NewsSpikeLimitPerDay = commonUtils.getConfigValue(ConfigConstants.NIFTY_50_NEWS_SPIKE_STOCK_LIMIT);
        if (nifty50NewsSpikeLimitPerDay == null) {
            throw new AlgotradeException(ErrorCodeConstants.CONFIG_MISSING_FOR_KEY);
        }
        List<StockMaster> nifty50Stock = newsSpikeStock.stream().filter(newsStock -> newsStock.isNifty50Stock()).limit(Integer.valueOf(nifty50NewsSpikeLimitPerDay)).collect(Collectors.toList());
        List<StockMaster> totalEligibleStocks = new ArrayList<>();
        if (!CollectionUtils.isEmpty(midCap100Stocks)) {
            totalEligibleStocks.addAll(midCap100Stocks);
        }
        if (!CollectionUtils.isEmpty(smallCap100Stocks)) {
            totalEligibleStocks.addAll(smallCap100Stocks);
        }
        if (!CollectionUtils.isEmpty(nifty500Stocks)) {
            totalEligibleStocks.addAll(nifty500Stocks);
        }
        if (!CollectionUtils.isEmpty(nifty50Stock)) {
            totalEligibleStocks.addAll(nifty50Stock);
        }
        return totalEligibleStocks;
    }

    public void processTopMoversEquityOrder(List<TopGainerLooserEntity> eligibleTopMovers) {
        eligibleTopMovers.forEach(etp -> {
            Optional<CandleEntity> firstCompletedCandle = TradeUtils.getStockCandle(etp.getStockSymbol()).values().stream().filter(CandleEntity::isCandleComplete).findFirst();
            CandleEntity candleEntity = firstCompletedCandle.get();
            if (etp.isTodaysTopGainer() && topGainerOrderCount.get() <= 2) {
                // prepareEquityOrder(etp, candleEntity.getLtp());
                logger.info("Placing order for the Top Gainer {},Instrument Token {}, Top Gainer Order Count {}", etp.getStockSymbol(), etp.getInstrumentToken(), topGainerOrderCount.get());
                topGainerOrderCount.getAndIncrement();
            } else if (topLooserOrderCount.get() <= 2) {
                OrderRequest orderRequest = new OrderRequest();
                orderRequest.setInstrumentToken(etp.getInstrumentToken());
                orderRequest.setTransactionType(TransactionType.SELL);
                orderRequest.setQuantity(5);
                SlDetails slDetails = new SlDetails();
                slDetails.setTriggerPrice(candleEntity.getLtp().add(TradeUtils.getXPercentOfY(new BigDecimal("0.5"), candleEntity.getLtp())));
                orderRequest.setSlDetails(slDetails);
                TargetDetails targetDetails = new TargetDetails();
                targetDetails.setTargetPrice(candleEntity.getLtp().subtract(TradeUtils.getXPercentOfY(new BigDecimal("1.5"), candleEntity.getLtp())));
                orderRequest.setTargetDetails(targetDetails);
                // prepareEquityOrder(orderRequest, Strategy.EQUITY_TOP_MOVERS.toString());
                tradeUtils.getWebsocketInstruments().remove(etp.getInstrumentToken());
                logger.info("Placing order for the Top Looser {},Instrument Token {}, TopLooser Order Count {}", etp.getStockSymbol(), etp.getInstrumentToken(), topLooserOrderCount.get());
                topLooserOrderCount.getAndIncrement();
            }
        });
        if (topGainerOrderCount.getAndAdd(topLooserOrderCount.get()) == 4) {
            periodicExecutorService.schedulerEqutiyWatchingPositions(periodicExecutorService);
        }

        //equityStrategyBuilderService.buildEquityStrategy(eligibleTopMovers, Strategy.EQUITY_TOP_MOVERS);

    }
}
