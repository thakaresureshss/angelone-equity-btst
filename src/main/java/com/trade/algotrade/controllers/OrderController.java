package com.trade.algotrade.controllers;

import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;
import com.trade.algotrade.client.kotak.response.LtpResponse;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.request.ManualOrderRequest;
import com.trade.algotrade.request.OrderFilterQuery;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.request.StrategyOrderRequest;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.service.InstrumentService;
import com.trade.algotrade.service.OrderService;
import com.trade.algotrade.service.StrategyBuilderService;
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
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    OrderService orderService;

    @Autowired
    KotakClient kotakClient;

    @Autowired
    InstrumentService instrumentService;

    @Autowired
    StrategyBuilderService strategyBuilderService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderResponse>> createOrders(@RequestBody OrderRequest orderRequest) {
        logger.info(" **** [CREATE ORDER][REQUEST] ********* Request : {}", orderRequest);
        List<OrderResponse> createdOrder = orderService.createOrder(orderRequest);
        logger.info("**** [CREATE ORDER][RESPONSE]********* Order Response {}", createdOrder);
        return new ResponseEntity<>(createdOrder, HttpStatus.OK);
    }





    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderResponse>> modifyOrder(@RequestBody OrderRequest orderDto, Long orderId) {
        logger.info(" **** [MODIFY ORDER][REQUEST] ********* Request : {}", orderDto);
        List<OrderResponse> modifiedOrders = orderService.modifyOrder(orderId, orderDto, null, true);
        logger.info("**** [MODIFY ORDER][RESPONSE]********* Order Response {}", modifiedOrders);
        return new ResponseEntity<>(modifiedOrders, HttpStatus.OK);
    }

    @GetMapping(value = "/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable("orderId") Long orderId,
                                                      @RequestParam("userId") String userId) {
        logger.info(" **** [GET ORDER][REQUEST] ********* Order Id : {}{}", orderId, userId);
        OrderResponse createdOrder = orderService.getOrder(orderId, userId);
        logger.info("**** [GET ORDER][RESPONSE]********* Order Response {}", createdOrder);
        return new ResponseEntity<>(createdOrder, HttpStatus.OK);
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value ="orderStatus", required = false) OrderStatus orderStatus,
            @RequestParam(value ="orderType", required = false) OrderType orderType,
            @RequestParam(value ="orderCategory", required = false) OrderCategoryEnum orderCategory,
            @RequestParam(value ="transactionType", required = false) TransactionType transactionType,
            @RequestParam(value ="segment", required = false) Segment segment,
            @RequestParam(value ="orderState") OrderState orderState) {

        logger.info(" **** [GET ALL ORDERS][REQUEST] *********");
        OrderFilterQuery orderFilterQuery = OrderFilterQuery.builder().userId(userId).orderStatus(orderStatus).orderType(orderType).orderCategory(orderCategory).transactionType(transactionType).segment(segment).orderState(orderState).build();
        List<OrderResponse> allOrder = orderService.getAllOrdersByFilter(orderFilterQuery);
        logger.info("**** [GET ALL ORDERS][RESPONSE]********* Total Orders {} Found ", allOrder);
        return new ResponseEntity<>(allOrder, HttpStatus.OK);
    }

    @DeleteMapping(value = "/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> cancelOrder(@PathVariable("orderId") Long orderId,
                                            @RequestParam("userId") String userId) {
        logger.info(" **** [CANCEL ORDER][REQUEST] ********* Order Id : {}{}", orderId, userId);
        orderService.cancelOrder(orderId, userId);
        logger.info("**** [CANCEL ORDER][RESPONSE]*********");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "/strategy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> getStrategyMapping(@RequestBody StrategyOrderRequest strategyOrderRequest) {
        long start = System.currentTimeMillis();
        logger.info(" **** [STRATEGY ORDER][REQUEST] ********* Strategy Order Request {}", strategyOrderRequest);
        LiveFeedWSMessage webSocketMessage = LiveFeedWSMessage.builder().build();
        webSocketMessage.setOptionType(strategyOrderRequest.getOptionType());
        webSocketMessage.setInstrumentName(Constants.BANK_NIFTY_INDEX);
        instrumentService.findByInstrumentName(Constants.BANK_NIFTY_INDEX);
        AngelOneInstrumentMasterEntity findByInstrumentName = instrumentService
                .findByInstrumentName(Constants.BANK_NIFTY_INDEX);
        if (findByInstrumentName == null) {
            return null;
        }
        LtpResponse ltp = kotakClient.getLtp(String.valueOf(findByInstrumentName.getInstrumentToken()));
        if (ltp == null || CollectionUtils.isEmpty(ltp.getSuccess())) {
            return null;
        }
        Optional<LtpSuccess> findFirst = ltp.getSuccess().stream()
                .filter(l -> l.getInstrumentToken().compareTo(findByInstrumentName.getInstrumentToken()) == 0)
                .findFirst();
        if (findFirst.isEmpty()) {
            return null;
        }
        BigDecimal lastPrice = findFirst.get().getLastPrice();
        webSocketMessage.setLtp(lastPrice);
        strategyBuilderService.buildBigCandleStrategy(webSocketMessage, strategyOrderRequest.getStrategy());
        long end = System.currentTimeMillis();

        logger.info("**** [STRATEGY ORDER][RESPONSE] Completed in  {} Millis *********", end - start);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(value="/test",produces = MediaType.APPLICATION_JSON_VALUE)
    public void modifyOrderNew() {
        logger.info(" **** [MODIFY ORDER][REQUEST] ********* Request : {}");
         orderService.modifyOrderFromController();
        logger.info("**** [MODIFY ORDER][RESPONSE]********* Order Response {}");
//        return new ResponseEntity<>(modifiedOrders, HttpStatus.OK);
    }

    @GetMapping(value="/all/angel",produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrderResponse> getAllAngelOrders(@RequestParam("userId") String userId){
        logger.info(" **** [GET ORDERS][REQUEST] ********* Request : {}");
        List<OrderResponse> allOrders = orderService.getAllOrders(userId);
        logger.info("**** [GET ORDERS][RESPONSE]********* Response {}");
        return allOrders;
    }

    @PostMapping(value="/manual",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<OrderResponse>> createManualOrder(@RequestBody ManualOrderRequest orderRequest) {
        logger.info(" **** [CREATE MANUAL ORDER ][REQUEST] ********* Request : {}", orderRequest);
        List<OrderResponse> createdOrder = strategyBuilderService.createManualOrder(orderRequest);
        logger.info("**** [CREATE MANUAL ORDER][RESPONSE]********* Order Response {}", createdOrder);
        return new ResponseEntity<>(createdOrder, HttpStatus.OK);
    }



}