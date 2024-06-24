package com.trade.algotrade.scheduler;

import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.enums.BrokerEnum;
import com.trade.algotrade.service.CandleService;
import com.trade.algotrade.service.NotificationService;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.service.WebsocketService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.TradeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class WebsocketConnectonScheduler {

    @Autowired
    CommonUtils commonUtils;

    private final Logger log = LoggerFactory.getLogger(WebsocketConnectonScheduler.class);

    @Autowired
    WebsocketService websocketService;

    @Autowired
    CandleService candleService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    UserService userService;


    @Async
    @Scheduled(cron = "${scheduler.websocket.interval}")
    public void connectAngelWebsocket() {
        log.debug(" ****** [ Angel Login ] [connectWebsocket ] Started ********");
        if (commonUtils.isTodayNseHoliday()) {
            return;
        }
        try {
            websocketService.connectAndSubscribeWatchInstrument();
        } catch (Exception e) {
            log.error(" ****** [ Angel Login ] [connectWebsocket ][Live Feed] ******** Exception {}", e.getMessage());
        }

        try {
            if (CommonUtils.getWebsocketOnFlag()) {
                websocketService.connectOrderFeedWebsocket();
            }
        } catch (Exception e) {
            log.error(" ****** [ Kotak Login ] [connectWebsocket ] [Order Feed] ******** Exception {}", e.getMessage());
        }
    }


    @Async
    @Scheduled(cron = "${scheduler.angelone-websocket.reconnect-interval}")
    public void reconnectAngelOneWebsocket() {
        log.debug("**************** WEBSOCKET RE-CONNECTION SCHEDULER STARTED ***************");
        if (commonUtils.isTodayNseHoliday()) {
            return;
        }
        if (!TradeUtils.isWebsocketConnected) {
            List<CandleEntity> candles = candleService.findCandlesBySymbolsDescending(Constants.BANK_NIFTY_INDEX);
            log.info("Current Websocket Connected {}", TradeUtils.isWebsocketConnected);
            connectWebsocket(candles);
        }
        if (!TradeUtils.isOrderFeedConnected) {
            log.info("Current Order Feed Connected {}", TradeUtils.isOrderFeedConnected);
            connectOrderFeed();
        }
        log.debug("=========== WEBSOCKET RE-CONNECTION SCHEDULER COMPLETED ==============");
    }

    private void connectWebsocket(List<CandleEntity> candlesBySymbolsDescending) {
        if (CollectionUtils.isEmpty(candlesBySymbolsDescending)) {
            Optional<CandleEntity> first = candlesBySymbolsDescending.stream().findFirst();
            if (first.isEmpty()) {
                TradeUtils.isWebsocketConnected = false;
            } else {
                int candleTimeCheck = 5;
                LocalDateTime fiveMinute = LocalDateTime.now().minusMinutes(candleTimeCheck);
                if (first.isPresent() && fiveMinute.isAfter(first.get().getStartTime())) {
                    log.info("Older Candle Found than {} Before of {} Will Reconnect Websocket.", first.get().getStartTime(), fiveMinute);
                    userService.getAllActiveUsersByBroker(BrokerEnum.ANGEL_ONE.toString()).forEach(userResponse -> notificationService.sendTelegramNotification(userResponse.getTelegramChatId(), "Angelone websocket disconnected"));
                    TradeUtils.isWebsocketConnected = false;
                } else {
                    log.info("Candles are Up to date,No need to reconnect websocket seems its connected, Current Websocket Connected {}", TradeUtils.isWebsocketConnected);
                    TradeUtils.isWebsocketConnected = true;
                }
            }

        } else {
            TradeUtils.isWebsocketConnected = true;
            connectAngelWebsocket();
            log.info("Seems Websocket already connected {}, Returning from flow ", TradeUtils.isWebsocketConnected);
        }

        if (!TradeUtils.isWebsocketConnected) {
            try {
                websocketService.connectAndSubscribeWatchInstrument();
                log.info(" =========== WEBSOCKET RE-CONNECTED SUCCESSFULLY ============= ");
            } catch (Exception e) {
                log.error("WEBSOCKET RECONNECT ERROR : {} ", e.getMessage());
            }
        }
    }

    private void connectOrderFeed() {
        try {
            if (CommonUtils.getWebsocketOnFlag()) {
                websocketService.connectOrderFeedWebsocket();
                log.info(" =========== ORDER FEED RECONNECTED SUCCESSFULLY ============= ");
            }
        } catch (Exception e) {
            log.error("ORDER FEED RECONNECT ERROR : {} ", e.getMessage());
        }
    }

    public void disconnectAndSubscribeAngelOneWebsocket() {
        websocketService.disconnectAngelOneWebsocket();
    }


    public void subscribeAngelOneWebsocketMock(Long instrumentToken) {
        websocketService.subscribeAngelOneWebsocketMock(instrumentToken);
    }

    public void subscribeTestTokenInAngelWebsocket(long instrumentToken) {
        websocketService.connectAndSubscribeInstrument(instrumentToken);
    }

    public void unsubscribeTestTokenInAngelWebsocket(long instrumentToken) {
        websocketService.unsubscribeInstrument(instrumentToken);
    }
}