package com.trade.algotrade.task;

/**
 * @author Suresh Thakare
 */

import com.trade.algotrade.client.kotak.LiveFeedWsClientEndpoint;
import com.trade.algotrade.constants.WebSocketConstant;
import com.trade.algotrade.task.service.PeriodicPingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;

public class PingMessageTask implements Runnable {
	Session session;
	long interval;
	PeriodicPingService periodicPingService;

	public PingMessageTask(Session session, long interval, PeriodicPingService periodicPingService) {
		super();
		this.session = session;
		this.interval = interval;
		this.periodicPingService = periodicPingService;
	}

	private static final Logger logger = LoggerFactory.getLogger(LiveFeedWsClientEndpoint.class);

	@Override
	public void run() {
		if (session.isOpen() && interval > 0) {
			try {
				session.getBasicRemote().sendText(WebSocketConstant.WS_PING_PAYLOAD);
			} catch (IOException e) {
				logger.info("client couldn't send Ping to server " + e.getMessage());
			}
		} else {
			logger.info("Connection is closed cancelling pingTask ");
			periodicPingService.cancelPingMessageTask();
		}
	}
}