package com.trade.algotrade.service;

import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import org.springframework.scheduling.annotation.Async;

public interface ManualSchedulerService {

    //@Async
    void processLtpMessage(LiveFeedWSMessage message);

	//void orderFeedManualScheduler();

	@Async
	void orderMonitoringScheduler();
}
