package com.trade.algotrade.controllers;

import com.trade.algotrade.client.nse.enums.ExchangeEquityFileNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.trade.algotrade.enitiy.equity.StockMaster;
import com.trade.algotrade.service.equity.StockMasterService;

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
public class EquityController {
    private static final Logger logger = LoggerFactory.getLogger(EquityController.class);

    @Autowired
    StockMasterService stockMasterService;

    @PostMapping(value = "/equity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addStock(@RequestBody StockMaster stockMaster, HttpServletRequest request) {
        logger.info(" **** [CREATE EQUITY ][REQUEST] *********{}", stockMaster);
        stockMasterService.addStock(stockMaster);
        logger.info("**** [CREATE EQUITY][RESPONSE]*********");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/insert/stocks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> downloadAndInsertStocks(@RequestParam("exchangeFileName") String exchangeEquityFileNames, Integer indexStockCount) {
        logger.info(" **** [CREATE EQUITY ][REQUEST] ********* Index Type {}, Index Stock Count {}", exchangeEquityFileNames, indexStockCount);
        stockMasterService.readNseStockCsv(exchangeEquityFileNames, indexStockCount);
        logger.info("**** [CREATE EQUITY][RESPONSE]*********");
        return new ResponseEntity<>(HttpStatus.OK);
    }

}