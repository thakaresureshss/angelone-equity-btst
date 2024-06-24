package com.trade.algotrade.controllers;

import com.trade.algotrade.request.ManualOrderRequest;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.service.StrategyBuilderService;
import com.trade.algotrade.service.WebsocketService;
import com.trade.algotrade.utils.TradeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * This class acts as a controller to handle request related to Country.
 *
 * @author suresh.thakare
 */

@RestController
@Validated
@RequestMapping(value = "/api/v1/mock")
@CrossOrigin(origins = "*")
public class MockController {
    private static final Logger logger = LoggerFactory.getLogger(MockController.class);

    @Autowired
    StrategyBuilderService strategyBuilderService;

    @Autowired
    WebsocketService websocketService;


    @PostMapping(value="/orderfeed/push",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderResponse>> pushOrderFeedMessage(@RequestBody String orderFeed) {
        logger.info("Pushing Order Feed to Queue", orderFeed);
        TradeUtils.orderFeedSet.clear();
        websocketService.onOrderStatusUpdate(orderFeed);
        logger.info("Pushing Order Feed to Queue", orderFeed);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value="/orderfeed/poll",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderResponse>> pollOrderFeedMessage() {
        logger.info("Processing Order Feed to Queue");
        strategyBuilderService.processOrderFeed();
        logger.info("Processed Order Feed to Queue");
        return new ResponseEntity<>(HttpStatus.OK);
    }

}