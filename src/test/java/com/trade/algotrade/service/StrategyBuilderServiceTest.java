package com.trade.algotrade.service;

import com.trade.algotrade.AlgoTradeTestDtoFactory;
import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.dto.EntryCondition;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.dto.websocket.OrderFeedWSMessage;
import com.trade.algotrade.enitiy.*;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.StrategyResponse;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.impl.StrategyBuilderServiceImpl;
import com.trade.algotrade.sort.InstrumentStrikePriceComparator;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StrategyBuilderServiceTest {

    @InjectMocks
    StrategyBuilderService strategyBuilderService = new StrategyBuilderServiceImpl();
    @Mock
    private StrategyService strategyService;
    @Mock
    private WebsocketService websocketService;
    @Mock
    private InstrumentService instrumentService;
    @Mock
    private CommonUtils commonUtils;

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock MarginService marginService;

    @Mock KotakClient kotakClient;

    private List<AngelOneInstrumentMasterEntity> kotakInstrumentMasterEntityList;
    private LiveFeedWSMessage webSocketMessage;
    private StrategyResponse strategyResponse;
    private List<UserResponse> userResponses;
    private List<OrderResponse> orderResponses;
    private OrderRequest sellOrderRequest;
    private OrderRequest orderRequest;

    @Mock
    TradeService ctradeOrderService;


    private AlgoTradeTestDtoFactory algoTradeTestDtoFactory;


    @Mock
    InstrumentStrikePriceComparator instrumentStrikePriceComparator;

    @BeforeEach
    void setUp() throws Exception {

        algoTradeTestDtoFactory = new AlgoTradeTestDtoFactory();
        BigDecimal indexSpotPrice = new BigDecimal(41020);
        CandleEntity candleeEntity = new CandleEntity();
        candleeEntity.setStartTime(DateUtils.getCurrentDateTimeIst());
        candleeEntity.setEndTime(DateUtils.getCurrentDateTimeIst().plusMinutes(3L));
        candleeEntity.setOpen(new BigDecimal(41020));
        candleeEntity.setHigh(new BigDecimal(41030));
        candleeEntity.setLow(new BigDecimal(40950));
        candleeEntity.setLtp(indexSpotPrice);
        candleeEntity.setClose(new BigDecimal(40970));

        String candleStartTime = DateUtils.getCurrentDateTimeIst().format(DateUtils.getCandledatetimeformatter());
        TradeUtils.updateBankNiftyCandelMap(candleStartTime, candleeEntity);

        candleeEntity = new CandleEntity();
        candleeEntity.setStartTime(DateUtils.getCurrentDateTimeIst().plusMinutes(3L));
        candleeEntity.setEndTime(DateUtils.getCurrentDateTimeIst().plusMinutes(6L));
        candleeEntity.setOpen(new BigDecimal(40980));
        candleeEntity.setHigh(new BigDecimal(41000));
        candleeEntity.setLow(new BigDecimal(40940));
        candleeEntity.setClose(new BigDecimal(40960));
        candleeEntity.setLtp(indexSpotPrice);
        candleStartTime = DateUtils.getCurrentDateTimeIst().plusMinutes(3L)
                .format(DateUtils.getCandledatetimeformatter());
        TradeUtils.updateBankNiftyCandelMap(candleStartTime, candleeEntity);

        kotakInstrumentMasterEntityList = new ArrayList<>();
        AngelOneInstrumentMasterEntity kotakInstrumentMasterEntity = AngelOneInstrumentMasterEntity.builder().instrumentToken(11717L)
                .strike(40890).build();
        kotakInstrumentMasterEntityList.add(kotakInstrumentMasterEntity);
        kotakInstrumentMasterEntity = AngelOneInstrumentMasterEntity.builder().instrumentToken(29126L).strike(40900).build();
        kotakInstrumentMasterEntityList.add(kotakInstrumentMasterEntity);
        kotakInstrumentMasterEntity = AngelOneInstrumentMasterEntity.builder().instrumentToken(29126L).strike(41100).build();
        kotakInstrumentMasterEntityList.add(kotakInstrumentMasterEntity);
        kotakInstrumentMasterEntity = AngelOneInstrumentMasterEntity.builder().instrumentToken(29126L).strike(40800).build();
        kotakInstrumentMasterEntityList.add(kotakInstrumentMasterEntity);

        webSocketMessage = LiveFeedWSMessage.builder().instrumentToken(11717L).ltp(new BigDecimal(40890))
                .instrumentName(Constants.BANK_NIFTY_INDEX).build();

        strategyResponse = new StrategyResponse();
        strategyResponse.setStrategyName(Strategy.BIGCANDLE.toString());
        Map<String, String> conditions = new HashMap<String, String>();
        conditions.put(Constants.BIG_CANDLE_POINTS, "70");
        EntryCondition condition = new EntryCondition();
        condition.setConditions(conditions);
        strategyResponse.setEntryCondition(condition);
        instrumentStrikePriceComparator = new InstrumentStrikePriceComparator();

        userResponses = new ArrayList<>();
        UserResponse userResponse = new UserResponse();
        userResponse.setUserId("ST27101989");
        Map<String, BigDecimal> dayLossLimit = new HashMap<>();
        dayLossLimit.put("FNO", new BigDecimal(500));
        userResponse.setDayLossLimit(dayLossLimit);

        userResponses.add(userResponse);

        OrderStatasticsEntity orderStatasticsEntity = OrderStatasticsEntity.builder().instrumentToken(11717L)
                .lossPrice(null).build();

        orderResponses = new ArrayList<>();

        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setPrice(new BigDecimal(80));

        orderResponses.add(orderResponse);

        orderRequest = OrderRequest.builder().instrumentToken(29126L).optionType(OptionType.CE)
                .orderType(OrderType.MARKET).transactionType(TransactionType.BUY)
                .userId("ST27101989").quantity(25).strikePrice(40900).build();

        sellOrderRequest = OrderRequest.builder().instrumentToken(29126L).transactionType(TransactionType.SELL)
                .optionType(OptionType.CE).orderType(OrderType.LIMIT).userId("ST27101989")
                .quantity(25).price(new BigDecimal(80)).build();

        StrategyEnity strategyEntity = new StrategyEnity();
        EntryCondition entryCondition = new EntryCondition();

        Map<String, String> map = new HashMap<>();
        map.put("stopLossPercentage", "20");
        map.put(Constants.DEFAULT_QUANTITY, "25");
        map.put(Constants.BIG_CANDLE_POINTS, "100");
        entryCondition.setConditions(map);

        strategyEntity.setEntryCondition(entryCondition);

        strategyResponse = new StrategyResponse();
        strategyResponse.setEntryCondition(entryCondition);

    }

    // @Test
    void testBuildStrategyWithBigCandle() {
        when(instrumentService.getAllInstruments()).thenReturn(kotakInstrumentMasterEntityList);
        when(userService.getAllActiveUsers()).thenReturn(userResponses);
        when(orderService.createOrder(orderRequest)).thenReturn(orderResponses);
        //TODO Need to fix this test case
        //when(tradeOrderService.getTodaysOpenTradesByStrategyAndUserIdIn(orderRequest)).thenReturn(orderResponses);
//		when(strategyRepository.findByStrategyname(Constants.BIG_CANDLE_STRATEGY)).thenReturn(strategyEntity);
//		when(strategyMapper.mapStrategyResponse(strategyEntity)).thenReturn(strategyResponse);
        when(strategyService.getStrategy(Strategy.BIGCANDLE.toString())).thenReturn(strategyResponse);
        strategyBuilderService.buildBigCandleStrategy(webSocketMessage, Strategy.BIGCANDLE);
        verify(instrumentService, times(1)).getAllInstruments();
    }

    @Disabled
    void testBuildStrategyWithBigCandleLessThanHundred() {
        String key = TradeUtils.getNiftyBankCandle().keySet().iterator().next();
        CandleEntity candleeEntity = TradeUtils.getNiftyBankCandle().get(key);
        candleeEntity.setOpen(new BigDecimal(41020));
        candleeEntity.setHigh(new BigDecimal(41040));
        candleeEntity.setLow(new BigDecimal(41000));
        candleeEntity.setClose(new BigDecimal(41040));
        TradeUtils.updateBankNiftyCandelMap(key, candleeEntity);

        when(strategyService.getStrategy(Strategy.BIGCANDLE.toString())).thenReturn(strategyResponse);
        strategyBuilderService.buildBigCandleStrategy(webSocketMessage, Strategy.BIGCANDLE);
        assertTrue(candleeEntity.getHigh().subtract(candleeEntity.getOpen()).compareTo(new BigDecimal(100)) < 0,
                "Price");
    }

    // @Test
    void testBuildStrategyWithExpiry() {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrderId(10001L);
        List<OrderResponse> orderResponses = List.of(orderResponse);
        LiveFeedWSMessage websocketMessageLocal = LiveFeedWSMessage.builder().instrumentToken(11717L)
                .ltp(new BigDecimal(41125)).instrumentName(Constants.BANK_NIFTY_INDEX).build();
        when(instrumentService.getAllInstruments()).thenReturn(kotakInstrumentMasterEntityList);
        when(orderService.createOrder(any())).thenReturn(orderResponses);
        strategyBuilderService.buildBigCandleStrategy(websocketMessageLocal, Strategy.EXPIRY);
        assertEquals(0, orderResponse.getOrderId().compareTo(10001L));
    }

    private StrategyResponse buildExpiryStrategyResponse() {
        StrategyResponse strategyResponse = new StrategyResponse();
        EntryCondition entryCondition = new EntryCondition();
        Map<String, String> conditions = new HashMap<String, String>();
        conditions.put(Constants.LAST_ORDER_QUANTITY, "900");
        conditions.put(Constants.DEFAULT_QUANTITY, "200");
        entryCondition.setConditions(conditions);
        strategyResponse.setEntryCondition(entryCondition);
        return strategyResponse;
    }

    //	@Test
    void testAnalyseOrders() throws InterruptedException {
        when(userService.getUsers()).thenReturn(userResponses);
        when(orderService.modifyOrder(2230406090644L, sellOrderRequest, null, false)).thenReturn(orderResponses);
//		when(orderService.createOrder(orderRequest)).thenReturn(orderResponses);
//		int max = 300;
//		int min = 150;
//		long start = System.currentTimeMillis();
//		long end = start + (2 * 60 * 1000); // 2 Minute *60 Second * 1 Second(1000millis)
//		while (System.currentTimeMillis() < end) {
//			Random random = new Random();
//			int ltp = random.nextInt(max - min) + min;
//			strategyBuilderService.analyseOrders(29126L, new BigDecimal(ltp));
//			Thread.sleep(10l);
//		}
//		strategyBuilderService.analyseOrders(29126L, new BigDecimal(180));

        verify(orderService, times(1)).modifyOrder(2230406090644L, sellOrderRequest, null, false);
    }

    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If no open orders are present and Monitor position method called,
     * Then  :
     * Expected  : It should return without calling userService.getAllActiveUsers() method
     */

    @Test
    void testMonitorPositionsWithNoOpenOrder() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(11717L).ltp(new BigDecimal(40890))
                .instrumentName(Constants.BANK_NIFTY_INDEX).build();
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(userService, times(0)).getAllActiveUsers();
    }

    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If null live feed messaged received as input to monitor position method called,
     * Then  :
     * Expected  : It should return without calling userService.getAllActiveUsers() method
     */

    @Test
    void testMonitorPositionsWithNullLiveOrderFeedMessage() {
        LiveFeedWSMessage liveFeedWSMessage = null;
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(userService, times(0)).getUserById(anyString());
    }


    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If there is no order today and monitor position method called,
     * Then  :
     * Expected  : It should return without calling userService.getUserById(userId+) method
     */


    @Test
    void testMonitorPositionsWithNoOrdersToday() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(11717L).ltp(new BigDecimal(40890))
                .instrumentName(Constants.BANK_NIFTY_INDEX).build();
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(userService, times(0)).getUserById(anyString());
    }


    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If there is active user in order and monitor position method called,
     * Then  :
     * Expected  : It should return without calling orderService.getTodaysAllOrdersByUserId() method
     */


    @Test
    void testMonitorPositionsWithNonEmptyTodaysOrder() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(11717L).ltp(new BigDecimal(40890))
                .instrumentName(Constants.BANK_NIFTY_INDEX).build();
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        when(orderService.getTodaysOrdersByInstrumentTokenAndStatus(any(Long.class), any(OrderStatus.class))).thenReturn(algoTradeTestDtoFactory.getTodaysOpenOrders());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(1)).getTodaysAllOrdersByUserId(anyString());
    }


    @Test
    void testMonitorPositionsWithSingleOpenOrderWithSingleUser() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(11717L).ltp(new BigDecimal(40890))
                .instrumentName(Constants.BANK_NIFTY_INDEX).build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        when(orderService.getTodaysOrdersByInstrumentTokenAndStatus(any(Long.class), any(OrderStatus.class))).thenReturn(algoTradeTestDtoFactory.getTodaysOpenOrders());
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(userService, times(1)).getUserById(anyString());
    }

    @Test
    void testMonitorPositionsWithSingleExecutedOrderWithNoSlOrderWithSingleUser() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(11717L).ltp(new BigDecimal(40890))
                .instrumentName(Constants.BANK_NIFTY_INDEX).build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        when(orderService.getTodaysOrdersByInstrumentTokenAndStatus(any(Long.class), any(OrderStatus.class))).thenReturn(todaysOpenOrder);
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(userService, times(1)).getUserById(anyString());
        verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }

    @Test
    void testMonitorPositionsWithSingleExecutedOrderWithSLOrderWithSingleUser() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(29126L).ltp(new BigDecimal(60))
                .build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        List<OpenOrderEntity> todaysOpenOrdersByUserId = algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId();
        todaysOpenOrdersByUserId.addAll(algoTradeTestDtoFactory.getTodaysSLOrder());
        when(orderService.getTodaysOrdersByInstrumentTokenAndStatus(any(Long.class), any(OrderStatus.class))).thenReturn(todaysOpenOrder);
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(todaysOpenOrdersByUserId);
//        doNothing().when(orderService).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }


    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If one user having multiple SL orders open and one of them is matched SL trigger conditions, Other is not matched
     * Then  :
     * Expected  : It should satisfy this mean it should modify only one SL order to Market which is eligible
     * verify(orderService, times(1)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
     */
    @Test
    void testMonitorPositionsWithSingleExecutedOrderWithMultiSLOrderWithSingleUserSLTriggeredForOneSlOnly() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(29126L).ltp(new BigDecimal(70))
                .build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        List<OpenOrderEntity> todaysOpenOrdersByUserId = algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId();
        List<OpenOrderEntity> todaysSLOrder = algoTradeTestDtoFactory.getTodaysSLOrder();
        SlDetails slDetails = new SlDetails();
        slDetails.setSlPrice(new BigDecimal(65));
        OpenOrderEntity slOrder = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(80))
                .validity(ValidityEnum.GFD)
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .slDetails(slDetails)
                .instrumentToken(29126L)
                .orderId(2230406090646L)
                .originalOrderId(2230406090644L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.OPEN)
                .orderCategory(OrderCategoryEnum.SQAREOFF)
                .build();
        todaysSLOrder.add(slOrder);
        todaysOpenOrdersByUserId.addAll(todaysSLOrder);
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(todaysOpenOrdersByUserId);
        when(orderService.getTodaysOrdersByInstrumentTokenAndStatus(any(Long.class), any(OrderStatus.class))).thenReturn(todaysOpenOrder);
        doNothing().when(orderService).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(1)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }


    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If one user having 2  SL orders open and all of them are matched SL trigger conditions
     * Then  :
     * Expected  : It should satisfy this mean it should modify two SL orders to Market which are eligible
     * verify(orderService, times(2)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
     */
    @Test
    void testMonitorPositionsWithSingleExecutedOrderWithMultiSLOrderWithSingleUserSLTriggeredForBoth() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(29126L).ltp(new BigDecimal(64))
                .build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        List<OpenOrderEntity> todaysOpenOrdersByUserId = algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId();
        List<OpenOrderEntity> todaysSLOrder = algoTradeTestDtoFactory.getTodaysSLOrder();
        SlDetails slDetails = new SlDetails();
        slDetails.setSlPrice(new BigDecimal(75));
        OpenOrderEntity slOrder = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(64))
                .validity(ValidityEnum.GFD)
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .slDetails(slDetails)
                .instrumentToken(29126L)
                .orderId(2230406090646L)
                .originalOrderId(2230406090644L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.OPEN)
                .orderCategory(OrderCategoryEnum.SQAREOFF)
                .strategyName(Strategy.BIGCANDLE.toString())
                .SlDetailsHistory(Collections.EMPTY_LIST)
                .build();
        todaysSLOrder.add(slOrder);
        todaysOpenOrdersByUserId.addAll(todaysSLOrder);
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(todaysOpenOrdersByUserId);
        when(orderService.getTodaysOrdersByInstrumentTokenAndStatus(any(Long.class), any(OrderStatus.class))).thenReturn(todaysOpenOrder);
//        doNothing().when(orderService).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }


    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If one user having 2  SL orders open and all of them are not matched SL trigger conditions
     * Then  :
     * Expected  : It should satisfy this mean it should not modify any SL order to Market which are eligible
     * verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
     */
    @Test
    void testMonitorPositionsWithSingleExecutedOrderWithMultiSLOrderWithSingleUserSLTriggeredForNone() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(29126L).ltp(new BigDecimal(80))
                .build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        when(orderService.getTodaysOrdersByInstrumentTokenAndStatus(any(Long.class), any(OrderStatus.class))).thenReturn(todaysOpenOrder);
        List<OpenOrderEntity> todaysOpenOrdersByUserId = algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId();
        List<OpenOrderEntity> todaysSLOrder = algoTradeTestDtoFactory.getTodaysSLOrder();
        SlDetails slDetails = new SlDetails();
        slDetails.setSlPrice(new BigDecimal(75));
        OpenOrderEntity slOrder = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(64))
                .validity(ValidityEnum.GFD)
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .slDetails(slDetails)
                .instrumentToken(29126L)
                .orderId(2230406090646L)
                .originalOrderId(2230406090644L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.OPEN)
                .orderCategory(OrderCategoryEnum.SQAREOFF)
                .build();
        todaysSLOrder.add(slOrder);
        todaysOpenOrdersByUserId.addAll(todaysSLOrder);
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(todaysOpenOrdersByUserId);
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }


    // Multi User :

    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If 2 user having 1 Primary Open Orders and there is no  SL orders available for both the users
     * Then  :
     * Expected  : It should satisfy this mean it should not modify any SL order to Market which are eligible
     * verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
     */
    @Test
    void testMonitorPositionsWithSingleExecutedOrderWithNoSlOrderWithMultiUser() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(11717L).ltp(new BigDecimal(40890))
                .instrumentName(Constants.BANK_NIFTY_INDEX).build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        SlDetails slDetails = new SlDetails();
        slDetails.setTriggerPrice(new BigDecimal(64));
        OpenOrderEntity marketOrder = OpenOrderEntity.builder()
                .userId("User2")
                .transactionType(TransactionType.BUY)
                .orderType(OrderType.MARKET)
                .quantity(25)
                .price(new BigDecimal(80))
                .validity(ValidityEnum.GFD)
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .slDetails(slDetails)
                .instrumentToken(29126L)
                .orderId(2230406090643L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.OPEN)
                .build();
        todaysOpenOrder.add(marketOrder);
        UserResponse userResponse1 = algoTradeTestDtoFactory.getUserResponse();
        UserResponse userResponse2 = algoTradeTestDtoFactory.getUserResponse();
        userResponse2.setUserId("User2");
        when(userService.getUserById(anyString())).thenReturn(userResponse1, userResponse2);
        when(orderService.getTodaysOrdersByInstrumentTokenAndStatus(any(Long.class), any(OrderStatus.class))).thenReturn(todaysOpenOrder);
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }


    @Test
    void testMonitorPositionsWithSingleExecutedOrderWithSLOrderWithMultiUser() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(29126L).ltp(new BigDecimal(60))
                .build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        UserResponse userResponse1 = algoTradeTestDtoFactory.getUserResponse();
        UserResponse userResponse2 = algoTradeTestDtoFactory.getUserResponse();
        userResponse2.setUserId("User2");
        when(userService.getUserById(anyString())).thenReturn(userResponse1, userResponse2);
        List<OpenOrderEntity> todaysOpenOrdersByUserId = algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId();
        List<OpenOrderEntity> todaysSLOrder = algoTradeTestDtoFactory.getTodaysSLOrder();
        todaysOpenOrdersByUserId.addAll(todaysSLOrder);
        when(orderService.getTodaysAllOrdersByUserId("User1")).thenReturn(todaysOpenOrdersByUserId);
        List<OpenOrderEntity> todaysOpenOrdersByUser2 = new ArrayList<>();
        SlDetails slDetailsUser2 = new SlDetails();
        slDetailsUser2.setSlPrice(new BigDecimal(79));
        SlDetails slDetails = new SlDetails();
        slDetails.setSlPrice(new BigDecimal(79));
        OpenOrderEntity marketOrder = OpenOrderEntity.builder()
                .userId("User2")
                .transactionType(TransactionType.BUY)
                .orderType(OrderType.MARKET)
                .quantity(25)
                .price(new BigDecimal(80))
                .validity(ValidityEnum.GFD)
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .slDetails(slDetails)
                .instrumentToken(29126L)
                .orderId(2230406090643L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.EXECUTED)
                .strategyName(Strategy.BIGCANDLE.toString())
                .build();
        todaysOpenOrdersByUser2.add(marketOrder);
        todaysOpenOrder.add(marketOrder);
        OpenOrderEntity slOrder = OpenOrderEntity.builder()
                .userId("User2")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(60))
                .validity(ValidityEnum.GFD)
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .slDetails(slDetailsUser2)
                .instrumentToken(29126L)
                .orderId(2230406090645L)
                .originalOrderId(2230406090643L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.OPEN)
                .orderCategory(OrderCategoryEnum.SQAREOFF)
                .strategyName(Strategy.BIGCANDLE.toString())
                .SlDetailsHistory(Collections.EMPTY_LIST)
                .build();
        todaysOpenOrdersByUser2.add(slOrder);
        todaysOpenOrder.add(slOrder);
        when(orderService.getTodaysAllOrdersByUserId("User2")).thenReturn(todaysOpenOrdersByUser2);
        when(orderService.getTodaysOrdersByInstrumentTokenAndStatus(any(Long.class), any(OrderStatus.class))).thenReturn(todaysOpenOrder);
//        doNothing().when(orderService).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }


    //TODO Needs to continue on this, Then Cover dri





    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If one user having multiple SL orders open and one of them is matched SL trigger conditions, Other is not matched
     * Then  :
     * Expected  : It should satisfy this mean it should modify only one SL order to Market which is eligible
     * verify(orderService, times(1)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
     */
    @Disabled
    void testMonitorPositionsWithMultiExecutedOrderWithMultiSLOrderPerUserOnlyOneSlTriggered() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(29126L).ltp(new BigDecimal(60))
                .build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        List<OpenOrderEntity> todaysOpenOrdersByUserId = algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId();
        List<OpenOrderEntity> todaysSLOrder = algoTradeTestDtoFactory.getTodaysSLOrder();
        SlDetails slDetails = new SlDetails();
        slDetails.setTriggerPrice(new BigDecimal(50));
        slDetails.setTriggerPrice(new BigDecimal(75));
        OpenOrderEntity slOrder = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(80))
                .validity(ValidityEnum.GFD)
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .slDetails(slDetails)
                .instrumentToken(29126L)
                .orderId(2230406090646L)
                .originalOrderId(2230406090644L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.OPEN)
                .orderCategory(OrderCategoryEnum.SQAREOFF)
                .build();
        todaysSLOrder.add(slOrder);
        todaysOpenOrdersByUserId.addAll(todaysSLOrder);
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(todaysOpenOrdersByUserId);
        doNothing().when(orderService).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(1)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }


    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If one user having 2  SL orders open and all of them are matched SL trigger conditions
     * Then  :
     * Expected  : It should satisfy this mean it should modify two SL orders to Market which are eligible
     * verify(orderService, times(2)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
     */
    @Disabled
    void testMonitorPositionsWithMultiExecutedOrderWithMultiSLOrderWithMultiUserAllSLTriggered() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(29126L).ltp(new BigDecimal(64))
                .build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        List<OpenOrderEntity> todaysOpenOrdersByUserId = algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId();
        List<OpenOrderEntity> todaysSLOrder = algoTradeTestDtoFactory.getTodaysSLOrder();
        SlDetails slDetails = new SlDetails();
        slDetails.setTriggerPrice(new BigDecimal(75));
        OpenOrderEntity slOrder = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(64))
                .validity(ValidityEnum.GFD)
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .slDetails(slDetails)
                .instrumentToken(29126L)
                .orderId(2230406090646L)
                .originalOrderId(2230406090644L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.OPEN)
                .orderCategory(OrderCategoryEnum.SQAREOFF)
                .build();
        todaysSLOrder.add(slOrder);
        todaysOpenOrdersByUserId.addAll(todaysSLOrder);
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(todaysOpenOrdersByUserId);
        doNothing().when(orderService).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(2)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }


    /**
     * @apiNote This Test case will cover,
     * Given :
     * When : If one user having 2  SL orders open and all of them are not matched SL trigger conditions
     * Then  :
     * Expected  : It should satisfy this mean it should not modify any SL order to Market which are eligible
     * verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
     */
    @Disabled
    void testMonitorPositionsWithMultiExecutedOrderWithMultiSLOrderWithMultiUserNoSLTriggered() {
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder().instrumentToken(29126L).ltp(new BigDecimal(78))
                .build();
        List<OpenOrderEntity> todaysOpenOrder = algoTradeTestDtoFactory.getTodaysOpenOrders();
        when(userService.getUserById(anyString())).thenReturn(algoTradeTestDtoFactory.getUserResponse());
        List<OpenOrderEntity> todaysOpenOrdersByUserId = algoTradeTestDtoFactory.getTodaysOpenOrdersByUserId();
        List<OpenOrderEntity> todaysSLOrder = algoTradeTestDtoFactory.getTodaysSLOrder();
        SlDetails slDetails = new SlDetails();
        slDetails.setTriggerPrice(new BigDecimal(75));
        OpenOrderEntity slOrder = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(64))
                .validity(ValidityEnum.GFD)
                .variety(VarietyEnum.NORMAL)
                .slDetails(slDetails)
                .instrumentToken(29126L)
                .orderId(2230406090646L)
                .originalOrderId(2230406090644L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.OPEN)
                .orderCategory(OrderCategoryEnum.SQAREOFF)
                .build();
        todaysSLOrder.add(slOrder);
        todaysOpenOrdersByUserId.addAll(todaysSLOrder);
        when(orderService.getTodaysAllOrdersByUserId(anyString())).thenReturn(todaysOpenOrdersByUserId);
        strategyBuilderService.monitorPositions(liveFeedWSMessage);
        verify(orderService, times(0)).modifySlOrderToMarketOrder(any(OpenOrderEntity.class), anyString());
    }

    @Disabled
    void addSLAndMarginForPlacedOrderWithFILStatus() {
        OrderFeedWSMessage orderFeedWSMessage = OrderFeedWSMessage.builder().status("TRAD").userId("User1").orderId("1230913122283").build();

        OpenOrderEntity order = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(64))
                .orderStatus(OrderStatus.OPEN)
                .build();
        when(orderService.getOpenOrderByOrderId(any(Long.class))).thenReturn(order);
        when(marginService.getMargin(orderFeedWSMessage.getUserId())).thenReturn(BigDecimal.TEN);
        strategyBuilderService.addSLAndMarginForPlacedOrder(orderFeedWSMessage);
        verify(marginService, times(1)).getMargin(orderFeedWSMessage.getUserId());
    }

    @Disabled
    void addSLAndMarginForPlacedOrderWithCANStatus() {
        OrderFeedWSMessage orderFeedWSMessage = OrderFeedWSMessage.builder().status("CAN").userId("User1").orderId("1230913122283").build();

        OpenOrderEntity order = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(64))
                .orderStatus(OrderStatus.OPEN)
                .build();
        when(orderService.getOpenOrderByOrderId(any(Long.class))).thenReturn(order);
        when(orderService.saveOpenOrders(order)).thenReturn(order);
        strategyBuilderService.addSLAndMarginForPlacedOrder(orderFeedWSMessage);
        verify(orderService, times(1)).saveOpenOrders(order);
        verify(marginService, times(0)).getMargin(orderFeedWSMessage.getUserId());
    }

    @Disabled
    void addSLAndMarginForPlacedOrderWithOrderStatusExecuted() {
        OrderFeedWSMessage orderFeedWSMessage = OrderFeedWSMessage.builder().status("CAN").userId("User1").orderId("1230913122283").build();

        OpenOrderEntity order = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(64))
                .orderStatus(OrderStatus.EXECUTED)
                .build();
        when(orderService.getOpenOrderByOrderId(any(Long.class))).thenReturn(order);
        strategyBuilderService.addSLAndMarginForPlacedOrder(orderFeedWSMessage);
        verify(orderService, times(0)).saveOpenOrders(order);
        verify(marginService, times(0)).getMargin(orderFeedWSMessage.getUserId());
    }
}
