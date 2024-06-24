package com.trade.algotrade.controllers;

import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.service.SchedulerInstatiationService;
import com.trade.algotrade.service.WebsocketService;
import com.trade.algotrade.utils.CommonUtils;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author Rahul Pansare
 */
@RestController
@Validated
@RequestMapping(value = "/api/v1/websocket")
@CrossOrigin(origins = "*")
@Api(tags = {"Scheduler Endpoints"})
public class WebsocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketController.class);
    @Autowired
    WebsocketService websocketService;

    @Autowired
    CommonUtils commonUtils;

    @GetMapping(value = "/connect", produces = MediaType.APPLICATION_JSON_VALUE)
    public void connectWebsocket() {
        logger.info("**** Subscribing angel one websocket. ****");
        websocketService.connectWebsocket();
    }

    @GetMapping(value = "/disconnect", produces = MediaType.APPLICATION_JSON_VALUE)
    public void disconnectWebsocket() {
        logger.info("**** Disconnect angel one websocket. ****");
        websocketService.disconnectWebsocket();
    }

    @GetMapping(value = "/reconnect", produces = MediaType.APPLICATION_JSON_VALUE)
    public void reconnectWebsocket() {
        logger.info("Reconnecting websocket");
        websocketService.reconnectWebsocket();
    }


    @GetMapping(value = "/subscribe/{instrumentToken}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void subscribeOnly(@PathVariable(value="instrumentToken") Long instrumentToken) {
        logger.info("**** Subscribing websocket for instrument {}", instrumentToken);
        websocketService.subscribeInstrument(instrumentToken);
    }

    @GetMapping(value = "/subscribe/mock", produces = MediaType.APPLICATION_JSON_VALUE)
    public void startMockWebsocket() {
        logger.info("**** Subscribing angel one websocket. ****");
        websocketService.subscribeAngelOneWebsocketMock(Long.valueOf(commonUtils.getConfigValue(ConfigConstants.KOTAK_NIFTY_BANK_INSTRUMENT)));
    }

    @GetMapping(value = "/connect/{instrumentToken}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void connectAndSubscribe(@PathVariable(value="instrumentToken") Long instrumentToken) {
        logger.info("**** Subscribing angel one test websocket. **** {}", instrumentToken);
        websocketService.connectAndSubscribeInstrument(instrumentToken);
    }

    @GetMapping(value = "/connect/subscribe-watch-instrument", produces = MediaType.APPLICATION_JSON_VALUE)
    public void connectAndSubscribeWatchInstrument() {
        logger.info("**** Connect and subscribe angel one websocket. ****");
        websocketService.connectAndSubscribeWatchInstrument();
    }

    @GetMapping(value = "/unsubscribe/{instrumentToken}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void unsubscribeInstrument(@PathVariable(value="instrumentToken") Long instrumentToken) {
        logger.info("**** Unsubscribing angel one websocket. **** {}", instrumentToken);
        websocketService.unsubscribeInstrument(instrumentToken);
    }

}
