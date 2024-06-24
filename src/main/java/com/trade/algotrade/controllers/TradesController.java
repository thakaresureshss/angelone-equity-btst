package com.trade.algotrade.controllers;

import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;
import com.trade.algotrade.client.kotak.response.LtpResponse;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.enitiy.KotakInstrumentMasterEntity;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.request.OrderFilterQuery;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.request.StrategyOrderRequest;
import com.trade.algotrade.request.TraderFilterQuery;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.TradeSummaryResponse;
import com.trade.algotrade.service.InstrumentService;
import com.trade.algotrade.service.OrderService;
import com.trade.algotrade.service.StrategyBuilderService;
import com.trade.algotrade.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * This class acts as a controller to handle request related to Country.
 *
 * @author suresh.thakare
 */

@RestController
@Validated
@RequestMapping(value = "/api/v1/orders")
@CrossOrigin(origins = "*")
public class TradesController {
    private static final Logger logger = LoggerFactory.getLogger(TradesController.class);

    @Autowired
    TradeService tradeService;

    @GetMapping(value = "/trades/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TradeSummaryResponse> getTradeSummary(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "tradeStatus", required = false) TradeStatus tradeStatus,
            @RequestParam(value = "segment", required = false) Segment segment,
            @RequestParam(value = "tradeState", required = false) TradeState tradeState,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate) {

        logger.info(" **** [GET ALL ORDERS][REQUEST] *********");
        TraderFilterQuery traderFilterQuery = TraderFilterQuery.builder().userId(userId)
                .tradeStatus(tradeStatus)
                .fromDate(fromDate)
                .toDate(toDate)
                .tradeState(tradeState)
                .userId(userId)
                .segment(segment).build();
        TradeSummaryResponse tradeSummary = tradeService.getTradeSummary(traderFilterQuery);
        logger.info("**** [GET ALL ORDERS][RESPONSE]********* Total Orders {} Found ", tradeSummary);
        return new ResponseEntity<>(tradeSummary, HttpStatus.OK);
    }

}