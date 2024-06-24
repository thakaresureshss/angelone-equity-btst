package com.trade.algotrade.scheduler;

import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.enitiy.equity.TopGainerLooserEntity;
import com.trade.algotrade.enums.OrderType;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.service.StrategyBuilderService;
import com.trade.algotrade.service.equity.StockMasterService;
import com.trade.algotrade.sort.SortUtils;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@EnableAsync
public class AlgoTradeScheduler {

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    StockMasterService stockMasterService;

    @Autowired
    TradeUtils tradeUtils;


    @Autowired
    StrategyBuilderService strategyBuilderService;


    private final Logger logger = LoggerFactory.getLogger(AlgoTradeScheduler.class);

    @Async
    //@Scheduled(cron = "${scheduler.intra-day.interval}") // 9.13 IST Time
    public void prepareDataForTrade() {
        logger.info(" ****** [prepareDataForTrade] Called ********");

        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        List<TopGainerLooserEntity> topMovers = stockMasterService.findAndSaveTopMovers();

        if (!CollectionUtils.isEmpty(topMovers)) {
            for (TopGainerLooserEntity tm : topMovers) {
                logger.info("Top Movers Found with Name:= {} , Change Percent:= {} , LTP := {}, Previous Close {} ,Top {}", tm.getStockSymbol(), tm.getChangePercent(), tm.getLtp(), tm.getPreviousClose(), tm.isTodaysTopGainer() ? "TOP GAINER" : "TOP LOOSER");
            }
            Set<Long> equityInstrument = topMovers.stream().map(TopGainerLooserEntity::getInstrumentToken).collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(equityInstrument)) {
                logger.info("{} Top Movers instruments added to watchlist ", equityInstrument.size());
                tradeUtils.getWebsocketInstruments().addAll(equityInstrument);
            }
        }
        logger.info(" ****** [prepareDataForTrade] Completed ********");
    }

    @Async
    // @Scheduled(cron = "${scheduler.intra-dayTrade.interval}") // 9.20 IST Time
    public void morningNineTwentyTrade() {
        logger.info(" ****** [morningNineTwentyTrade] Called ********");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        List<TopGainerLooserEntity> topTenMoves = stockMasterService.getTopTenMoves();
        if (!CollectionUtils.isEmpty(topTenMoves)) {
            List<TopGainerLooserEntity> eligibleTopMovers = findEligibleTopMoversStocks(topTenMoves);
            if (CollectionUtils.isEmpty(eligibleTopMovers)) {
                logger.info("NO TOP MOVERS FOUND");
                return;
            }
            stockMasterService.processTopMoversEquityOrder(eligibleTopMovers);
        }
        logger.info(" ****** [morningNineTwentyTrade] Completed ********");
    }


    private List<TopGainerLooserEntity> findEligibleTopMoversStocks(List<TopGainerLooserEntity> topTenMoves) {
        return topTenMoves.stream().map(tp -> {
            Map<String, CandleEntity> stockCandles = TradeUtils.getStockCandle(tp.getStockSymbol());

            if (!CollectionUtils.isEmpty(stockCandles)) {
                logger.info(" ****** [findEligibleTopMoversStocks] Candle Found for {} Stock ********", tp.getStockSymbol());
                Optional<CandleEntity> firstCompletedCandle = stockCandles.values().stream().filter(CandleEntity::isCandleComplete).findFirst();
                CandleEntity candleEntity = firstCompletedCandle.get();
                Optional<TopGainerLooserEntity> topGainerOptional = stockMasterService.getTopTenGainer().stream().filter(tg -> tg.getInstrumentToken().equals(tp.getInstrumentToken())).findAny();

                Optional<TopGainerLooserEntity> topLooser = stockMasterService.getTopTenLooser().stream().filter(tg -> tg.getInstrumentToken().equals(tp.getInstrumentToken())).findAny();
                if (topGainerOptional.isPresent() && candleEntity.getOpen().compareTo(candleEntity.getLow()) == 0) {
                    tp.setTodaysTopGainer(true);
                    return tp;
                }
                if (topLooser.isPresent() && candleEntity.getOpen().compareTo(candleEntity.getHigh()) == 0) {
                    return tp;
                }
                logger.info(" ****** [findEligibleTopMoversStocks] Candle Found for {} Stock ********", tp.getStockSymbol());
            }
            return null;
        }).filter(Objects::nonNull).sorted(SortUtils.percentageChangeComparatorTopGainerLoosers().reversed()).collect(Collectors.toList());
    }

    private void setOrderCommonParams(OrderRequest orderRequest) {
        orderRequest.setOrderType(OrderType.MARKET);
        orderRequest.setProduct(com.trade.algotrade.client.angelone.enums.ProductType.MIS);
//        orderRequest.setValidity(ValidityEnum.GFD);
        orderRequest.setVariety(VarietyEnum.NORMAL);
    }


    @Async
    //@Scheduled(cron = "${scheduler.equityDayOpen-close.interval}")
    public void updateDayOpen() {
        logger.info(" ****** [updateEquityDayClose] Called ********");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        if (DateUtils.isBetweenPreMarketCloseAndMarketOpenTime()) {
            stockMasterService.updateDayOpen();

        }
        logger.info(" ****** [updateEquityDayClose] Completed ********");
    }


    @Async
    // @Scheduled(cron = "${scheduler.news-spike.interval}")
    public void findNewsSpikeStocksAndPlaceOrder() {
        logger.info(" ****** [updateAllTodaysNewsStock] Called ********");
/*        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }*/
        stockMasterService.findNewsSpikeStockAndSave();
        logger.info(" ****** [updateAllTodaysNewsStock] Completed ********");
    }


    @Async
    @Scheduled(cron = "${scheduler.bnf-bigmove.interval}")
    public void bigMoveStrategy() {
        logger.info("BNF BIG-MOVE : - SCHEDULER Called ********");
        if (commonUtils.isTodayNseHoliday()) {
            logger.info("[Today is Holiday]");
            return;
        }
        strategyBuilderService.buildBigMoveStrategy();
        logger.info("BNF BIG-MOVE : - SCHEDULER Called ********");
    }
}