package com.trade.algotrade.service.impl;

import com.trade.algotrade.client.angelone.AngelOneClient;
import com.trade.algotrade.client.kotak.LiveFeedWsClientEndpoint;
import com.trade.algotrade.client.kotak.OrderFeedWsClientEndpoint;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.scheduler.AlgoTradeScheduler;
import com.trade.algotrade.scheduler.LoginScheduler;
import com.trade.algotrade.scheduler.WebsocketConnectonScheduler;
import com.trade.algotrade.service.InstrumentService;
import com.trade.algotrade.service.NseService;
import com.trade.algotrade.service.SchedulerInstatiationService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.TradeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Rahul Pansare
 */
@Service
public class SchedulerInstantiationServiceImpl implements SchedulerInstatiationService {

    private final Logger logger = LoggerFactory.getLogger(SchedulerInstantiationServiceImpl.class);

    @Autowired
    private LoginScheduler kotakLoginScheduler;

    @Autowired
    private WebsocketConnectonScheduler websocketConnectonScheduler;

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private NseService nseService;

    @Autowired
    TradeUtils tradeUtils;

    @Autowired
    AngelOneClient angelOneClient;

    @Autowired
    private AlgoTradeScheduler algoTradeScheduler;

    @Autowired
    OrderFeedWsClientEndpoint clientEndpoint;

    @Autowired
    CommonUtils commonUtils;


    @Override
    public void initiateDownloadInstrument() {
        logger.debug("**** Initiating download instrument and store into DB manually from service called ****");
        kotakLoginScheduler.downloadAndSaveInstrument();
        logger.debug("**** Initiating download instrument and store into DB manually from service completed ****");
    }

    @Override
    public void initiateFetchHolidays() {
        logger.debug(" ******[SchedulerInstantiationServiceImpl][initiateFetchHolidays] Called ********");
        nseService.updateNseHolidays();
        logger.debug(" ****** [SchedulerInstantiationServiceImpl][initiateFetchHolidays] Completed ********");
    }

    @Override
    public void clearApplicationStaticData() {
        logger.debug(" ******[SchedulerInstantiationServiceImpl][updateNseHolidays] Called ********");
        kotakLoginScheduler.clearStaticAppDataScheduler();
        logger.debug(" ****** [SchedulerInstantiationServiceImpl][updateNseHolidays] Completed ********");
    }

    @Override
    public void squareOffIntraDayPositions() {
        logger.debug(" ******[SchedulerInstantiationServiceImpl][squareOffIntraDayPositions] Called ********");
        kotakLoginScheduler.squareOffFnoIntradayOrder();
        logger.debug(" ****** [SchedulerInstantiationServiceImpl][squareOffIntraDayPositions] Completed ********");
    }

    @Override
    public void loadMarginLocally() {
        logger.debug(" ******[SchedulerInstantiationServiceImpl][loadMarginLocally] Called ********");
        kotakLoginScheduler.loadMarginDataLocally();
        logger.debug(" ****** [SchedulerInstantiationServiceImpl][loadMarginLocally] Completed ********");
    }

    @Override
    public void moveCompletedTradesToHistoryDocuments() {
        logger.info(" ******[SchedulerInstantiationServiceImpl][moveCompletedTradesToHistoryDocuments] Called ********");
        kotakLoginScheduler.moveCompletedTradesToHistory();
        logger.info(" ****** [SchedulerInstantiationServiceImpl][moveCompletedTradesToHistoryDocuments] Completed ********");
    }

    @Override
    public void initiateAngelOneLogin() {
        logger.debug("**** Initiating Angel One login from service ****");
        kotakLoginScheduler.getAngelOneAccessToken();
    }

    @Override
    public void connectAndSubscribe() {
        TradeUtils.isLatestTrendFinderActive = true;
        websocketConnectonScheduler.connectAngelWebsocket();
    }

    @Override
    public void disconnectAndSubscribeAngelOneWebsocket() {
        websocketConnectonScheduler.disconnectAndSubscribeAngelOneWebsocket();
    }

    @Override
    public void placeNewsSpikeTrades() {
        logger.debug("**** NEWS Spike Equity Trade ****");
        algoTradeScheduler.findNewsSpikeStocksAndPlaceOrder();
    }

    @Override
    public void subscribeAngelOneWebsocketMock() {
        logger.info("**** subscribeAngelOneWebsocketMock Called ****");
        if (instrumentService.getAllInstruments().isEmpty() || instrumentService.getAngelKotakMapping().isEmpty())
            kotakLoginScheduler.downloadAndSaveInstrument();

        TradeUtils.isLatestTrendFinderActive = true;
        if (CommonUtils.getOffMarketHoursTestFlag()) {
            CommonUtils.pauseMockWebsocket = false;
        }
        websocketConnectonScheduler.subscribeAngelOneWebsocketMock(Long.valueOf(commonUtils.getConfigValue(ConfigConstants.KOTAK_NIFTY_BANK_INSTRUMENT)));
    }


    @Override
    public void eodProcess() {
        logger.info("**** eodProcess Called ****");
        kotakLoginScheduler.eodProcessScheduler();
    }

    @Override
    public void connectAndSubscribeToken(long instrumentToken) {
        websocketConnectonScheduler.subscribeTestTokenInAngelWebsocket(instrumentToken);
    }

    @Override
    public void unsubscribeTestTokenInAngelWebsocket(long instrumentToken) {
        websocketConnectonScheduler.unsubscribeTestTokenInAngelWebsocket(instrumentToken);
    }

    @Override
    public void initiateOpenInterest() {
        nseService.fetchOpenInterestOfInstruments();
    }

    @Override
    public void initiateReconnectAngelOneWebsocket() {
        websocketConnectonScheduler.reconnectAngelOneWebsocket();
    }


}
