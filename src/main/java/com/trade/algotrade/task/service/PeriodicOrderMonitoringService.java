package com.trade.algotrade.task.service;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.trade.algotrade.service.ManualSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trade.algotrade.task.OrderMonitoringTask;

public class PeriodicOrderMonitoringService {

	private final int THREAD_POOL_SIZE = 2;
	private volatile long orderInterval;
	private volatile ScheduledFuture<?> orderMonitoringTask;
	private final ScheduledExecutorService service = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);
	private static final Logger logger = LoggerFactory.getLogger(PeriodicManualLiveFeedService.class);
	
	public void cancelOrderMonitoringTask() {
		if(orderMonitoringTask != null && !orderMonitoringTask.isCancelled()) {
			logger.info("**** Canceling MANUAL ORDER MONITORING JOB(OrderMonitoringTask). ****");
			orderMonitoringTask.cancel(true);
		}
	}
	
	public void setMonitorOrderInterval(long interval, ManualSchedulerService manualSchedulerService) {
		logger.info(" Initialising MANUAL ORDER MONITORING JOB  Interval {}", interval);
		orderInterval = interval;
		if (orderInterval < 1) {
			return;
		}

		if (orderMonitoringTask != null && !orderMonitoringTask.isDone()) {
			logger.info("Already OrderMonitoringTask is running, No need to add new task, Hence returning");
			return;
		}
		logger.info("**** Scheduling MANUAL ORDER MONITORING JOB to monitor the orders. ****");
		orderMonitoringTask = service.scheduleAtFixedRate(new OrderMonitoringTask(orderInterval, manualSchedulerService),
				orderInterval, orderInterval, TimeUnit.MILLISECONDS);
		logger.info("MANUAL ORDER MONITORING JOB Initialization Completed for Interval {}", interval);
	}
}
