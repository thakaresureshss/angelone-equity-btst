package com.trade.algotrade.task;

import com.trade.algotrade.client.kotak.OrderFeedWsClientEndpoint;
import com.trade.algotrade.service.WebsocketService;
import com.trade.algotrade.task.service.PeriodicOrderWsRetryService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderFeedWSRetryTask implements Runnable {
    long interval;
    PeriodicOrderWsRetryService wsRetryService;
    WebsocketService websocketService;
    private static final Logger logger = LoggerFactory.getLogger(OrderFeedWSRetryTask.class);

    public OrderFeedWSRetryTask(long interval, PeriodicOrderWsRetryService wsRetryService, WebsocketService websocketService) {
        super();
        this.interval = interval;
        this.wsRetryService = wsRetryService;
        this.websocketService = websocketService;
    }

    @Override
    public void run() {
        logger.info(" **** RETRY ORDER WEBSOCKET Called **** ");
        try {
            websocketService.connectOrderFeed();
        } catch (Exception e) {
            logger.error("RETRY ORDER WEBSOCKET ERROR : {} ", e.getMessage());
        }
        logger.info(" **** RETRY ORDER WEBSOCKET Completed **** ");
    }
}