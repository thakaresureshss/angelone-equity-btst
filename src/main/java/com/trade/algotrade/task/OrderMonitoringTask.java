package com.trade.algotrade.task;

import com.trade.algotrade.client.kotak.OrderFeedWsClientEndpoint;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trade.algotrade.service.ManualSchedulerService;

public class OrderMonitoringTask implements Runnable {

    long interval;
    ManualSchedulerService manualSchedulerService;

    private static final Logger logger = LoggerFactory.getLogger(OrderMonitoringTask.class);

    public OrderMonitoringTask(long interval, ManualSchedulerService manualSchedulerService) {
        super();
        this.interval = interval;
        this.manualSchedulerService = manualSchedulerService;
    }

    @Override
    public void run() {
        if (!DateUtils.isTradingSessionTime() && !CommonUtils.getOffMarketHoursTestFlag()) {
            OrderFeedWsClientEndpoint.stopOrderMonitoringScheduler();
            logger.info("Closing order monitoring Task JOB,As Trading session is closed");
            return;
        }
        logger.debug("***[OrderMonitoringTask] [Order Monitoring JOB] ** CALLED ****");
        try {
            manualSchedulerService.orderMonitoringScheduler();
        } catch (Exception e) {
            logger.error("Exception in ltpScheduler :{}", e.getMessage());
        }
        logger.debug("*** [OrderMonitoringTask] [Order Monitoring JOB] ** COMPLETED ****");

    }
}
