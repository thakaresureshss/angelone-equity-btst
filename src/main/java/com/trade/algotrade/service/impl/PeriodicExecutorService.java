package com.trade.algotrade.service.impl;

import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class PeriodicExecutorService {

    private final int THREAD_POOL_SIZE = 4;
    private volatile ScheduledFuture<?> topMover;
    private final ScheduledExecutorService service = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);
    private final Logger logger = LoggerFactory.getLogger(PeriodicExecutorService.class);

    public void cancelTask() {
        topMover.cancel(true);
    }

    public void schedulerEqutiyWatchingPositions(PeriodicExecutorService periodicExecutorService) {
        logger.info(" ****** [schedulerEqutiySquareOffExecution] Called ******** ");
        if (!DateUtils.isTradingSessionTime() && !CommonUtils.getOffMarketHoursTestFlag()) {
            logger.info("Cancel > Scheduling Task > TopMoversSquareOffTask  Reason {}",
                    "Market Trading session is closed");
            cancelTask();
        }
        logger.info("Scheduling Task > TopMoversSquareOffTask");
        topMover = service.scheduleAtFixedRate(new TopMoversSquareOffTask(periodicExecutorService), 3, 3,
                TimeUnit.SECONDS);

    }
}
