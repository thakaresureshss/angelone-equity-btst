package com.trade.algotrade.service.impl;

import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;
import com.trade.algotrade.client.kotak.enums.OrderStatusWebsocket;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.client.kotak.request.TargetDetails;
import com.trade.algotrade.client.kotak.response.LtpResponse;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.dto.websocket.OrderFeedWSMessage;
import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enitiy.equity.StockMaster;
import com.trade.algotrade.enitiy.equity.TopGainerLooserEntity;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.repo.OpenOrderRepository;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.StrategyResponse;
import com.trade.algotrade.service.*;
import com.trade.algotrade.service.equity.StockMasterService;
import com.trade.algotrade.utils.TradeUtils;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
public class EquityStrategyBuilderServiceImpl implements EquityStrategyBuilderService {

    private final Logger logger = LoggerFactory.getLogger(EquityStrategyBuilderServiceImpl.class);

/*    @Autowired
    StockMasterService stockMasterService;*/

    @Autowired
    CandleService candleService;

    @Autowired
    OrderService orderService;

    @Autowired
    OpenOrderRepository openOrderRepository;

    @Autowired
    StrategyService strategyService;

    @Autowired
    KotakClient kotakClient;


    @Autowired
    UserStrategyService userStrategyService;

/*    @Override
    @Async
    public void buildEquityStrategy(LiveFeedWSMessage message, Strategy strategy) {
        if (strategy.toString().equals(Constants.STRATEGY_EQ_TOP_MOVER)) {
            logger.info(" ****** [buildEquityStrategy] Called Strategy {}", strategy);
            List<Long> instrumentTokens = new ArrayList<>();
            for (TopGainerLooserEntity tm : stockMasterService.getTopTenMoves()) {
                Long instrumentToken = tm.getInstrumentToken();
                instrumentTokens.add(instrumentToken);
            }
            if (!CollectionUtils.isEmpty(stockMasterService.getTopTenMoves())
                    && instrumentTokens.contains(message.getInstrumentToken())) {
                candleService.buildCandles(message.getInstrumentName(), message.getLtp());
            }
        }

    }*/

    @Override
    public void buildEquityStrategy(List<StockMaster> eligibleStocks, Strategy strategyName) {
        StrategyResponse strategy;
        try {
            strategy = strategyService.getStrategy(strategyName.toString());
        } catch (AlgotradeException algotradeException) {
            // Get the strategy configurations from DB for the input strategy, If input strategy not found in config then don't proceed.
            logger.info("**** No Configuration found for the strategy := {} , Error details: {} Hence Returning.", strategyName, algotradeException.getError());
            return;
        }

        switch (strategyName) {
            case EQUITY_NEWS_SPIKE:
                processNewsSpikeStockStrategy(eligibleStocks, strategy);
                break;
            default:
                break;
        }

    }

    private void processNewsSpikeStockStrategy(List<StockMaster> eligibleStocks, StrategyResponse strategy) {
        String ltpRequest = TradeUtils.getLtpRequest(eligibleStocks);
        LtpResponse ltpResponses = kotakClient.getLtp(ltpRequest);
        if (ltpResponses == null) {
            return;
        }
        List<LtpSuccess> lastPrices = ltpResponses.getSuccess();
        if (!CollectionUtils.isEmpty(lastPrices)) {
            Map<Long, LtpSuccess> ltpMap = lastPrices.stream()
                    .collect(Collectors.toMap(LtpSuccess::getInstrumentToken, Function.identity()));

            List<CompletableFuture<List<OrderResponse>>> allOrderProcessingThreads = new ArrayList<>();
            processEquityNewsSpikeStocks
                    (eligibleStocks, strategy, ltpMap, allOrderProcessingThreads);
            CompletableFuture.allOf(allOrderProcessingThreads.toArray(new CompletableFuture[0])).join();
        }
    }

    private void processEquityNewsSpikeStocks(List<StockMaster> eligibleStocks, StrategyResponse strategy, Map<Long, LtpSuccess> ltpMap, List<CompletableFuture<List<OrderResponse>>> allOrderProcessingThreads) {
        eligibleStocks.stream().forEach(stock -> {
            CompletableFuture<List<OrderResponse>> listCompletableFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return preOrderRequests(strategy, ltpMap, stock);
                } catch (final Exception e) {
                    logger.info("Equity Stock : Processing failed in {} Completable Exception   := {} , Error details", stock.getStockSymbol(), e.getMessage());
                }
                return null;
            }).thenApplyAsync(orderRequest -> {
                if (Objects.nonNull(orderRequest)) {
                    return orderService.prepareOrderAndCreateOrder(orderRequest);
                }
                return new ArrayList<>();
            });
            allOrderProcessingThreads.add(listCompletableFuture);
        });
    }

    private static OrderRequest preOrderRequests(StrategyResponse strategy, Map<Long, LtpSuccess> ltpMap, StockMaster stock) {
        LtpSuccess ltp = ltpMap.get(stock.getInstrumentToken());
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setSegment(Segment.EQ.toString());
        orderRequest.setStrategyName(strategy.getStrategyName());
        orderRequest.setInstrumentToken(stock.getInstrumentToken());
        orderRequest.setTransactionType(TransactionType.BUY);
        SlDetails slDetails = new SlDetails();
        BigDecimal lastPrice = ltp.getLastPrice();
        Map<String, String> getExitConfigMap = strategy.getExitCondition().getConditions();
        if (getExitConfigMap == null) {
            throw new AlgotradeException(ErrorCodeConstants.EXIT_CONDITION_MISSING);
        }
        String lossPercent = getExitConfigMap.get(ConfigConstants.NEWS_SPIKE_LOSS_PERCENT);
        if (StringUtils.isEmpty(lossPercent)) {
            throw new AlgotradeException(ErrorCodeConstants.NOT_FOUND_NEWS_SPIKE_LOSS_PERCENT);
        }
        slDetails.setTriggerPrice(lastPrice.subtract(TradeUtils.getXPercentOfY(new BigDecimal(lossPercent), lastPrice)));
        orderRequest.setSlDetails(slDetails);
        TargetDetails targetDetails = new TargetDetails();
        String gainPercent = getExitConfigMap.get(ConfigConstants.NEWS_SPIKE_PROFIT_PERCENT);
        if (StringUtils.isEmpty(gainPercent)) {
            throw new AlgotradeException(ErrorCodeConstants.NOT_FOUND_NEWS_SPIKE_PROFIT_PERCENT);
        }
        targetDetails.setTargetPrice(lastPrice.add(TradeUtils.getXPercentOfY(new BigDecimal(gainPercent), lastPrice)));
        orderRequest.setTargetDetails(targetDetails);
        orderRequest.setOrderCategory(OrderCategoryEnum.NEW);
        orderRequest.setOrderType(OrderType.MARKET);
        orderRequest.setPrice(BigDecimal.ZERO);
        return orderRequest;
    }

/*

    @Override
    @Async
    public void monitorEqutiyOrders(OrderFeedWSMessage message) {
        Optional<TopGainerLooserEntity> ordersFromTopMoves = stockMasterService.getTopTenMoves().stream()
                .filter(tm -> tm.getInstrumentToken().equals(message.getInstrumentToken())).findAny();
        if (ordersFromTopMoves.isPresent()) {
            List<OpenOrderEntity> openOrders = openOrderRepository.findAll();
            if (!CollectionUtils.isEmpty(openOrders)) {
                Optional<OpenOrderEntity> openOrderMatching = openOrders.stream()
                        .filter(oo -> oo.getInstrumentToken().equals(message.getInstrumentToken())).findAny();
                if (openOrderMatching.isPresent()
                        && message.getStatus().equalsIgnoreCase(OrderStatusWebsocket.FIL.toString())) {
                    OpenOrderEntity openOrderEntity = openOrderMatching.get();
                    OrderRequest orderRequest = new OrderRequest();
                    orderRequest.setSegment(Segment.EQ.toString());
                    // SL Order
                    setOrderCommonParams(orderRequest);
                    orderRequest.setInstrumentToken(openOrderEntity.getInstrumentToken());
                    orderRequest.setQuantity(openOrderEntity.getQuantity());
                    orderRequest.setStrategyName(openOrderEntity.getStrategyName());
                    orderRequest.setUserIds(Collections.singletonList(message.getUserId()));
                    orderRequest.setTransactionType(
                            openOrderEntity.getTransactionType() == TransactionType.SELL ? TransactionType.BUY
                                    : TransactionType.SELL);
                    if (openOrderEntity.getSlDetails() != null) {
                        orderRequest.setPrice(openOrderEntity.getSlDetails().getTriggerPrice());
                        orderService.createOrder(orderRequest);
                    }
                    if (openOrderEntity.getTargetDetails() != null) {
                        orderRequest.setPrice(openOrderEntity.getTargetDetails().getTargetPrice());
                        orderService.prepareOrderAndCreateEquityOrder(orderRequest);
                    }
                }
            }
        }
    }
*/

    private void setOrderCommonParams(OrderRequest orderRequest) {
        orderRequest.setOrderType(OrderType.MARKET);
        orderRequest.setProduct(com.trade.algotrade.client.angelone.enums.ProductType.MIS);
//        orderRequest.setValidity(ValidityEnum.GFD);
        orderRequest.setVariety(VarietyEnum.NORMAL);
    }
}
