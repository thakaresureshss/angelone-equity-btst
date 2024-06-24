package com.trade.algotrade.service.impl;

import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.enitiy.*;
import com.trade.algotrade.enums.CriteriaFilter;
import com.trade.algotrade.enums.OrderState;
import com.trade.algotrade.enums.TradeState;
import com.trade.algotrade.enums.TradeStatus;
import com.trade.algotrade.repo.TradeHistoryRepository;
import com.trade.algotrade.repo.TradeRepository;
import com.trade.algotrade.repo.filter.FilterBuilder;
import com.trade.algotrade.request.TraderFilterQuery;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.TradeResponse;
import com.trade.algotrade.response.TradeSummaryResponse;
import com.trade.algotrade.service.TradeService;
import com.trade.algotrade.sort.SortUtils;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TradeServiceImpl implements TradeService {

    private static final Logger logger = LoggerFactory.getLogger(TradeServiceImpl.class);

    @Autowired
    TradeRepository tradeRepository;

    @Autowired
    TradeHistoryRepository tradeHistoryRepository;

    @Autowired
    CommonUtils commonUtils;

    @Override
    // @Cacheable(value = "openTrades", key = "#userId")
    public List<TradeEntity> getTodaysTradeByUserId(String userId) {
        LocalDateTime marketOpenDateTime = DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME));
        logger.debug("***** getTradeByUserId called With User ID := {} and Created Date Time From {} *****", userId, marketOpenDateTime);
        return tradeRepository.findByUserIdAndCreatedTimeGreaterThanEqual(userId, marketOpenDateTime);
    }

    //  @CacheEvict(value = "openTrades", allEntries = true)
    public TradeEntity createTrade(TradeEntity tradeOrderEntity) {
        logger.debug("***** Saving Trade With User ID := {} , Details := {} *****", tradeOrderEntity.getUserId(), tradeOrderEntity);
        return tradeRepository.save(tradeOrderEntity);
    }

    //  @CacheEvict(value = "openTrades", allEntries = true)
    public TradeEntity modifyTrade(TradeEntity tradeOrderEntity) {
        logger.debug("***** Modified Trade With User ID := {} , Details := {} *****", tradeOrderEntity.getUserId(), tradeOrderEntity);
        return tradeRepository.save(tradeOrderEntity);
    }

    @Override
    // @Cacheable(value = "openTrades", key = "new org.springframework.cache.interceptor.SimpleKey(#userId, #instrumentToken)")
    public List<TradeEntity> getTradeByUserIdAndInstrumentTokenAndStrategy(String userId, Long instrumentToken, String strategyName) {
        logger.debug("***** getTradeByUserIdAndInstrumentTokenAndStrategy called With User ID := {} With  InstrumentToken := {} *****", userId, instrumentToken);
        return tradeRepository.findByUserIdAndInstrumentTokenAndTradeStatusAndCreatedTimeGreaterThanEqual(userId, instrumentToken, TradeStatus.OPEN, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    @Override
//    @Cacheable(value = "openTrades", key = "#strategyname")
    //TODO Need to cache this method  need to Revisit This
    public List<TradeEntity> getTodaysOpenTradesByStrategyAndUserIdIn(String strategyname, List<String> userIds) {
        logger.debug("***** getTodaysOpenTradesByStrategyAndUserIdIn called With Strategy := {} With Active Users := {} *****", strategyname, userIds);
        return tradeRepository.findByStrategyAndUserIdInAndTradeStatusAndCreatedTimeGreaterThanEqual(strategyname, userIds, TradeStatus.OPEN, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    @Override
    public BigDecimal getTodaysRealisedPnl(String userId) {
        List<TradeEntity> todaysTrades = getAllTodaysCompletedTrades(userId);
        if (!CollectionUtils.isEmpty(todaysTrades)) {
            return todaysTrades.stream().map(TradeEntity::getRealisedPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Optional<TradeEntity> getTradeById(String tradeId) {
        return tradeRepository.findByTradeId(tradeId);
    }

    @Override
    public void deleteTrade(TradeEntity tradeToDelete) {
        tradeRepository.delete(tradeToDelete);
    }

    @Override
    public Optional<TradeEntity> getTradeByUserIdAndInstrumentTokenAndStrategyAndTradeStatus(String userId, Long instrumentToken, String strategyName, TradeStatus tradeStatus) {
        return tradeRepository.findByStrategyAndUserIdAndTradeStatusAndCreatedTimeGreaterThanEqualAndInstrumentToken(strategyName, userId, tradeStatus, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)), instrumentToken);
    }

    @Override
    public List<OpenOrderEntity> getOpenOrderList(String tradeId) {
        if (tradeRepository.findByTradeId(tradeId).isPresent()) {
            TradeEntity tradeEntity = tradeRepository.findByTradeId(tradeId).get();
            return tradeEntity.getOrders();
        }
        return null;
    }

    @Override
    public Optional<TradeEntity> getTradeForOrder(String orderId) {
        return tradeRepository.getTradeForOrder(orderId);
    }

    @Override
    public boolean isOpenTradesFoundForToday() {
        List<TradeEntity> byTradeStatusAndCreatedTimeGreaterThanEqual = tradeRepository.findByTradeStatusAndCreatedTimeGreaterThanEqual(TradeStatus.OPEN, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
        return !CollectionUtils.isEmpty(byTradeStatusAndCreatedTimeGreaterThanEqual);
    }

    @Override
    public List<TradeHistoryEntity> moveTradesToHistory() {
        List<TradeEntity> completedTrades = tradeRepository.findByTradeStatus(TradeStatus.COMPLETED);
        if (!CollectionUtils.isEmpty(completedTrades)) {
            List<TradeHistoryEntity> historyOrders = completedTrades.stream().map(o -> {
                TradeHistoryEntity tradeHistoryEntity = TradeHistoryEntity.builder().build();
                BeanUtils.copyProperties(o, tradeHistoryEntity);
                return tradeHistoryEntity;
            }).collect(Collectors.toList());
            tradeHistoryRepository.saveAll(historyOrders);
            tradeRepository.deleteAll(completedTrades);
            return historyOrders;
        }
        return new ArrayList<>();
    }

    @Override
    public List<TradeEntity> getTradesByInstrumentAndStrategyAndStatus(Long instrument, String strategy) {
        List<TradeEntity> trades = tradeRepository.findByInstrumentTokenAndStrategyAndTradeStatusAndCreatedTimeGreaterThanEqual(instrument, strategy, TradeStatus.OPEN, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
        return trades;
    }

    @Override
    public List<TradeEntity> getAllTodaysCompletedTrades(String userId) {
        return tradeRepository.findByUserIdAndTradeStatusAndCreatedTimeGreaterThanEqual(userId, TradeStatus.COMPLETED, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    @Override
    public List<TradeEntity> getOpenTradesFoundForToday() {
        return tradeRepository.findByTradeStatusAndCreatedTimeGreaterThanEqual(TradeStatus.OPEN, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }

    @Override
    public List<TradeEntity> getAllTodaysTradesOrderByDesc(String userId) {
        return tradeRepository.findByUserIdAndTradeStatusAndCreatedTimeGreaterThanOrderByCreatedTimeDesc(userId, TradeStatus.COMPLETED, DateUtils.getMarketOpenDateTime(commonUtils.getConfigValue(ConfigConstants.MARKET_OPEN_TIME)));
    }


    @Override
    public TradeSummaryResponse getTradeSummary(TraderFilterQuery traderFilterQuery) {

        List<TradeResponse> filterdTrades = new ArrayList<>();
        FilterBuilder filterBuilder = new FilterBuilder();
        if (Objects.nonNull(traderFilterQuery.getUserId())) {
            filterBuilder.addFilter("userId", CriteriaFilter.eq, traderFilterQuery.getUserId());
        }
        if (Objects.nonNull(traderFilterQuery.getSegment())) {
            filterBuilder.addFilter("segment", CriteriaFilter.eq, traderFilterQuery.getSegment());
        }

        if (Objects.nonNull(traderFilterQuery.getFromDate())) {
            String fromDate = traderFilterQuery.getFromDate().concat(DateUtils.TIME_BLOCK_DEFAULT);
            filterBuilder.addFilter("createdTime", CriteriaFilter.gte, DateUtils.convertStringToLocalDateTime(fromDate, DateUtils.ALGO_TRADE_REQUEST_DATE_TO_LOCALDATETIME));
        }
        if (Objects.nonNull(traderFilterQuery.getToDate())) {
            String toDate = traderFilterQuery.getToDate().concat(DateUtils.TIME_BLOCK_DEFAULT);
            filterBuilder.addFilter("createdTime", CriteriaFilter.lte, DateUtils.convertStringToLocalDateTime(toDate, DateUtils.ALGO_TRADE_REQUEST_DATE_TO_LOCALDATETIME));
        }

        //For now this method will return either the active orders or history orders not all orders
        if (Objects.nonNull(traderFilterQuery.getTradeState()) && traderFilterQuery.getTradeState() == TradeState.HISTORY) {
            filterdTrades = tradeHistoryRepository.findAllByFiter(TradeHistoryEntity.class, filterBuilder).stream().map(historyTrade -> mapTradeEntityToTradeResponse(historyTrade)).collect(Collectors.toList());
        } else if (Objects.nonNull(traderFilterQuery.getTradeState()) && traderFilterQuery.getTradeState() == TradeState.ALL) {
            filterdTrades = tradeRepository.findAllByFiter(TradeEntity.class, filterBuilder).stream().map(tradeEntity -> mapTradeEntityToTradeResponse(tradeEntity)).collect(Collectors.toList());
            List<TradeResponse> historyTrades = tradeHistoryRepository.findAllByFiter(TradeHistoryEntity.class, filterBuilder).stream().map(tradeEntity -> mapTradeEntityToTradeResponse(tradeEntity)).collect(Collectors.toList());
            filterdTrades.addAll(historyTrades);
        } else {
            // Default It will return only active Orders
            filterdTrades = tradeRepository.findAllByFiter(TradeEntity.class, filterBuilder).stream().map(tradeEntity -> mapTradeEntityToTradeResponse(tradeEntity)).collect(Collectors.toList());
        }
        TradeSummaryResponse summaryResponse = new TradeSummaryResponse();
        if (!CollectionUtils.isEmpty(filterdTrades)) {
            TradeResponse tradeResponse = filterdTrades.stream().min(SortUtils.durationCountComparator()).get();
            TradeResponse maxDurationTrade = filterdTrades.stream().min(SortUtils.durationCountComparator().reversed()).get();
            summaryResponse.setMinTradeDuration(tradeResponse.getTradeDuration());
            summaryResponse.setMaxTradeDuration(maxDurationTrade.getTradeDuration());
            TradeResponse minUniqueTrendCount = filterdTrades.stream().min(SortUtils.uniqueTrendCountComparator()).get();
            TradeResponse maxUniqueTredCount = filterdTrades.stream().min(SortUtils.uniqueTrendCountComparator().reversed()).get();
            summaryResponse.setMinTrendUniqueCount(minUniqueTrendCount.getUniqueTrendCount());
            summaryResponse.setMaxTrendUniqueCount(maxUniqueTredCount.getUniqueTrendCount());
            summaryResponse.setTotalTrades(filterdTrades.size());
            summaryResponse.setTrades(filterdTrades);

            long successfulTrades = filterdTrades.stream().filter(t -> "SUCCESSFUL".equalsIgnoreCase(t.getSuccessStatus())).count();
            long failedTrades = filterdTrades.stream().filter(t -> "UNSUCCESSFUL".equalsIgnoreCase(t.getSuccessStatus())).count();
            summaryResponse.setFailedTrades((int) failedTrades);
            summaryResponse.setSuccessTrade((int) successfulTrades);
            
        }
        return summaryResponse;
    }

    private TradeResponse mapTradeEntityToTradeResponse(BaseTradeEntity tradeEntity) {
        TradeResponse tradeResponse = new TradeResponse();
        tradeResponse.setTradeDuration(tradeEntity.getTradeDuration());
        tradeResponse.setAverageTrendPoints(tradeEntity.getAverageTrendPoints());
        tradeResponse.setSegment(tradeEntity.getSegment());
        tradeResponse.setRealisedPnl(tradeEntity.getRealisedPnl());
        tradeResponse.setTradeStatus(tradeEntity.getTradeStatus());
        tradeResponse.setUserId(tradeEntity.getUserId());
        tradeResponse.setUniqueTrendCount(tradeEntity.getUniqueTrendCount());
        tradeResponse.setCreatedTime(tradeEntity.getCreatedTime());
        tradeResponse.setUpdatedTime(tradeEntity.getUpdatedTime());
        tradeResponse.setSuccessStatus(tradeEntity.getSuccessStatus());
        return tradeResponse;
    }
}
