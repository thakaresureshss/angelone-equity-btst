package com.trade.algotrade.task.service;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.websocket.Session;

import com.trade.algotrade.client.kotak.OrderFeedWsClientEndpoint;
import org.glassfish.tyrus.core.l10n.LocalizationMessages;

import com.trade.algotrade.client.kotak.LiveFeedWsClientEndpoint;
import com.trade.algotrade.task.PingMessageTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriodicPingService {

    private final int THREAD_POOL_SIZE = 2;
    private volatile long pingInterval;
    private volatile ScheduledFuture<?> pingPongTask;
    private final ScheduledExecutorService service = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);

    private static final Logger logger = LoggerFactory.getLogger(ScheduledExecutorService.class);

    public void cancelPingMessageTask() {
        if (pingPongTask != null && !pingPongTask.isCancelled()) {
            pingPongTask.cancel(true);
        }
    }

    public void setPingInterval(long interval, Session session, PeriodicPingService pingService) {
        logger.info("Star.ting Ping Scheduler for Order Feed Websocket");
        if (!LiveFeedWsClientEndpoint.getSession().isOpen()) {
            throw new IllegalStateException(LocalizationMessages.CONNECTION_HAS_BEEN_CLOSED());
        }
        pingInterval = interval;
        cancelPingMessageTask();
        if (pingInterval < 1) {
            return;
        }

        if (pingPongTask != null && !pingPongTask.isDone()) {
            logger.info("Already PingMessageTask is running, No need to add new task, Hence returning");
            return;
        }
        logger.info("Starting Ping Scheduler for Order Feed Websocket");
        pingPongTask = service.scheduleAtFixedRate(new PingMessageTask(session, pingInterval, pingService),
                pingInterval, pingInterval, TimeUnit.MILLISECONDS);
    }

    public void setOrderFeedPingInterval(long interval, Session session, PeriodicPingService pingService) {
        logger.info("Starting Ping Scheduler for Order Feed Websocket Session ID {} ", session.getId());
   /*     if (!OrderFeedWsClientEndpoint.getSession().isOpen()) {
            throw new IllegalStateException(LocalizationMessages.CONNECTION_HAS_BEEN_CLOSED());
        }*/
        pingInterval = interval;
        cancelPingMessageTask();
        if (pingInterval < 1) {
            return;
        }

        if (pingPongTask != null && !pingPongTask.isDone()) {
            logger.info("Already PingMessageTask is running, No need to add new task, Hence returning Session ID {} ", session.getId());
            return;
        }
        pingPongTask = service.scheduleAtFixedRate(new PingMessageTask(session, pingInterval, pingService),
                pingInterval, pingInterval, TimeUnit.MILLISECONDS);
    }

    public void setPingIntervalForOrderWebsocket(long interval, Session session, PeriodicPingService pingService) {
/*        if (!OrderFeedWsClientEndpoint.getSession().isOpen()) {
            throw new IllegalStateException(LocalizationMessages.CONNECTION_HAS_BEEN_CLOSED());
        }*/
        pingInterval = interval;
        cancelPingMessageTask();
        if (pingInterval < 1) {
            return;
        }

        if (pingPongTask != null && !pingPongTask.isDone()) {
            logger.info("Already PingMessageTask is running, No need to add new task, Hence returning");
            return;
        }

        pingPongTask = service.scheduleAtFixedRate(new PingMessageTask(session, pingInterval, pingService),
                pingInterval, pingInterval, TimeUnit.MILLISECONDS);
    }

}
