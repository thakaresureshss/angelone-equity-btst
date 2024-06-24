package com.trade.algotrade.controllers;

import com.trade.algotrade.request.OrderConfigUpdateRequest;
import com.trade.algotrade.service.UserStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trade.algotrade.request.StrategyRequest;
import com.trade.algotrade.response.StrategyResponse;
import com.trade.algotrade.service.StrategyService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * This class acts as a controller to handle request related to Country.
 *
 * @author suresh.thakare
 */

@RestController
@Validated
@RequestMapping(value = "/api/v1")
@CrossOrigin(origins = "*")
public class StrategyController {
    private static final Logger logger = LoggerFactory.getLogger(StrategyController.class);

    @Autowired
    StrategyService strategyService;

    @Autowired
    UserStrategyService userStrategyService;

    @PostMapping(value = "/strategy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StrategyResponse> createStrategy(@RequestBody StrategyRequest strategyRequest,
                                                           HttpServletRequest request) {
        logger.debug(" **** [CREATE STRATEGY][REQUEST] ********* Request : {}", strategyRequest);
        StrategyResponse createdStrategy = strategyService.createStrategy(strategyRequest);
        logger.debug("**** [CREATE STRATEGY][RESPONSE]********* STRATEGY Response {}", createdStrategy);
        return new ResponseEntity<>(createdStrategy, HttpStatus.OK);
    }

    @GetMapping(value = "/strategy/{strategyName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StrategyResponse> getStrategyByName(@PathVariable("strategyName") String strategyName,
                                                              HttpServletRequest request) {
        logger.debug(" **** [GET STRATEGY][REQUEST] ********* Request : {}", strategyName);
        StrategyResponse createdStrategy = strategyService.getStrategy(strategyName);
        logger.debug("**** [GET STRATEGY][RESPONSE]********* STRATEGY Response {}", createdStrategy);
        return new ResponseEntity<>(createdStrategy, HttpStatus.OK);
    }

    @PutMapping(value = "/strategy/{strategyName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StrategyResponse> modifyStrategy(@PathVariable("strategyName") String strategyName,
                                                           @RequestBody StrategyRequest strategyRequest, HttpServletRequest request) {
        logger.debug(" **** [MODIFY STRATEGY][REQUEST] *********Strategy Name{},  Request : {}", strategyName,
                strategyRequest);
        StrategyResponse createdStrategy = strategyService.modifyStrategy(strategyName, strategyRequest);
        logger.debug("**** [MODIFY STRATEGY][RESPONSE]********* Strategy Name{},  Response : {}", strategyName,
                createdStrategy);
        return new ResponseEntity<>(createdStrategy, HttpStatus.OK);
    }

    @DeleteMapping(value = "/strategy/{strategyName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> cancelOrder(@PathVariable("strategyName") String strategyName) {
        logger.debug(" **** [Delete Strategy][REQUEST] ********* strategy Name : { }", strategyName);
        strategyService.deleteStrategy(strategyName);
        logger.debug("**** [Delete Strategy][RESPONSE]*********");
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping(value = "/strategy/{strategyName}/updateOrderConfig", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateOrderConfig(@PathVariable("strategyName") String strategyName,
                                                              @RequestBody OrderConfigUpdateRequest orderConfigUpdateRequest, HttpServletRequest request) {
        logger.debug(" **** [MODIFY STRATEGY][updateOrderConfig] ********* Strategy Name {},  Request : {}", strategyName,
                orderConfigUpdateRequest);
        userStrategyService.updateOrderConfig(strategyName,orderConfigUpdateRequest);
        logger.debug("**** [MODIFY STRATEGY][RESPONSE]********* Strategy Name{}", strategyName);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}