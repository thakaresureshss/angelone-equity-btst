package com.trade.algotrade.task.service;

import com.trade.algotrade.client.kotak.OrderFeedWsClientEndpoint;
import com.trade.algotrade.service.WebsocketService;
import com.trade.algotrade.task.OrderFeedWSRetryTask;
import com.trade.algotrade.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PeriodicOrderWsRetryService {

    private final int THREAD_POOL_SIZE = 2;
    private volatile long wsRetryInterval;
    private volatile ScheduledFuture<?> retryWSConnectTask;
    private final ScheduledExecutorService retryService = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);
    private static final Logger logger = LoggerFactory.getLogger(PeriodicOrderWsRetryService.class);

    public void cancelWsRetryTask(String reason, Session websocket) {
        if (retryWSConnectTask != null && !retryWSConnectTask.isCancelled()) {
            logger.info("[cancelWsRetryTask] Cancelling Order Feed Retry as Retry task was running {} User ID {}", reason, websocket.getUserPrincipal());
            retryWSConnectTask.cancel(true);
        }
    }

    public void setRetryOrderWsInterval(OrderFeedWsClientEndpoint orderFeedWsClientEndpoint, long interval, PeriodicOrderWsRetryService retryService,
                                        WebsocketService websocketService) {
        logger.info("[setRetryWsInterval] Called Interval {} for order", interval);
        wsRetryInterval = interval;
        if (wsRetryInterval < 1) {
            return;
        }
        if (retryWSConnectTask != null && !retryWSConnectTask.isDone()) {
            logger.info("Already OrderFeedWSRetryTask is running, No need to add new task, Hence returning");
            return;
        }
        retryWSConnectTask = this.retryService.scheduleAtFixedRate(
                new OrderFeedWSRetryTask(wsRetryInterval, retryService, websocketService), wsRetryInterval, wsRetryInterval,
                TimeUnit.MILLISECONDS);
        logger.info("[setRetryWsInterval] Completed Interval {}", interval);
    }

    public long getPingInterval() {
        return wsRetryInterval;
    }
}
