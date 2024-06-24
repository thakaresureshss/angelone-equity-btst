package com.trade.algotrade.controllers;

import com.trade.algotrade.response.InstrumentResponse;
import com.trade.algotrade.service.InstrumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping(value = "/api/v1/instrument")
@CrossOrigin(origins = "*")
public class InstrumentController {

    @Autowired
    InstrumentService instrumentService;

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @GetMapping(value = "/{instrumentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstrumentResponse> getOrderById(@PathVariable("instrumentId") Long instrumentId) {
        logger.info(" **** [GET INSTRUMENT][REQUEST] ********* INSTRUMENT Id : {}", instrumentId);
        InstrumentResponse instrumentResponse = instrumentService.getInstrumentByTokenTest(instrumentId);
        logger.info("**** [GET INSTRUMENT][RESPONSE]********* INSTRUMENT Response {}", instrumentResponse);
        return new ResponseEntity<>(instrumentResponse, HttpStatus.OK);
    }
}
