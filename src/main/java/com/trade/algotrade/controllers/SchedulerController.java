package com.trade.algotrade.controllers;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.trade.algotrade.service.SchedulerInstatiationService;

/**
 * @author Rahul Pansare
 */
@RestController
@Validated
@RequestMapping(value = "/api/v1")
@CrossOrigin(origins = "*")
@Api(tags = {"Scheduler Endpoints"})
public class SchedulerController {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerController.class);

    @Autowired
    private SchedulerInstatiationService schedulerInstatiationService;


    @GetMapping(value = "/scheduler/download-instrument", produces = MediaType.APPLICATION_JSON_VALUE)
    public void initiateDownloadInstrument() {
        logger.debug("**** Initiating download instrument manually from controller. ****");
        schedulerInstatiationService.initiateDownloadInstrument();
    }

    @GetMapping(value = "/scheduler/holidays", produces = MediaType.APPLICATION_JSON_VALUE)
    public void initiateFetchHolidays() {
        logger.debug("**** Initiating fetching holidays manually from controller. ****");
        schedulerInstatiationService.initiateFetchHolidays();
    }


    @GetMapping(value = "/scheduler/clear-static-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public void clearStaticData() {
        logger.debug("**** Initiating fetching holidays manually from controller. ****");
        schedulerInstatiationService.clearApplicationStaticData();
    }


    @GetMapping(value = "/scheduler/intra-day-squareoff", produces = MediaType.APPLICATION_JSON_VALUE)
    public void squareOffIntraDayPositions() {
        logger.debug("**** Initiating fetching holidays manually from controller. ****");
        schedulerInstatiationService.squareOffIntraDayPositions();
    }

    @GetMapping(value = "/scheduler/margin", produces = MediaType.APPLICATION_JSON_VALUE)
    public void loadMarginLocally() {
        logger.debug("**** Initiating fetching holidays manually from controller. ****");
        schedulerInstatiationService.loadMarginLocally();
    }


    @GetMapping(value = "/scheduler/trades/moveCompleted", produces = MediaType.APPLICATION_JSON_VALUE)
    public void moveCompletedTradesToHistory() {
        logger.debug("**** Initiating fetching holidays manually from controller. ****");
        schedulerInstatiationService.moveCompletedTradesToHistoryDocuments();
    }

    @GetMapping(value = "/scheduler/login/angelone", produces = MediaType.APPLICATION_JSON_VALUE)
    public void initiateAngelOneLogin() {
        logger.debug("**** Initiating AngelOne login manually from controller. ****");
        schedulerInstatiationService.initiateAngelOneLogin();
    }


    @GetMapping(value = "/scheduler/equity/orders/news-spike", produces = MediaType.APPLICATION_JSON_VALUE)
    public void findAndPlaceOrderForNewsSpikeStocks() {
        logger.info("**** Find News Spike Trade Stock  and Place orders for them ****");
        schedulerInstatiationService.placeNewsSpikeTrades();
    }


    @GetMapping(value = "/scheduler/initiate/openInterest", produces = MediaType.APPLICATION_JSON_VALUE)
    public void initiateOpenInterest() {
        logger.info("**** Initiating open interest call. **** ");
        schedulerInstatiationService.initiateOpenInterest();
    }
    @GetMapping(value = "/scheduler/eod-process", produces = MediaType.APPLICATION_JSON_VALUE)
    public void eodProcess() {
        logger.info("**** Subscribing angel one websocket. ****");
        schedulerInstatiationService.eodProcess();
    }

}
