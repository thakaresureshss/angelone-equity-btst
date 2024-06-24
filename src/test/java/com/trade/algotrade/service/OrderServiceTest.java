package com.trade.algotrade.service;

import com.trade.algotrade.AlgoTradeTestDtoFactory;
import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.request.KotakOrderRequest;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.dto.EntryCondition;
import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enitiy.TradeEntity;
import com.trade.algotrade.enitiy.UserStrategyEntity;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.exceptions.AlgoValidationException;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.exceptions.KotakValidationException;
import com.trade.algotrade.repo.OpenOrderRepository;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.response.OrderResponse;
import com.trade.algotrade.response.StrategyResponse;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.impl.OrderServiceImpl;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import com.trade.algotrade.utils.WebsocketUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    OrderService orderService = new OrderServiceImpl();
    @Mock
    private InstrumentService instrumentService;

    @Mock
    private UserService userService;

    @Mock
    TradeService tradeService;

    @Mock
    MarginService marginService;

    @Mock
    OpenOrderRepository openOrderRepository;

    private AlgoTradeTestDtoFactory algoTradeTestDtoFactory;

    @Mock
    KotakClient kotakClient;

    @Mock
    private WebsocketUtils websocketUtils;
    @Mock
    private UserStrategyService userStrategyService;

    @Mock
    private StrategyService strategyService;
    @Mock
    private TradeUtils tradeUtils;
    @Mock
    private WebsocketService websocketService;
    @Mock
    private AlgoConfigurationService algoConfigurationService;

    @BeforeEach
    void setUp() {
        algoTradeTestDtoFactory = new AlgoTradeTestDtoFactory();
        Mockito.lenient().when(instrumentService.getAllInstruments()).thenReturn(algoTradeTestDtoFactory.getAllInstruments());
        Mockito.lenient().when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(Collections.EMPTY_LIST);
        Mockito.lenient().when(tradeService.createTrade(any(TradeEntity.class))).thenReturn(algoTradeTestDtoFactory.createNewTradeResponse());
        Mockito.lenient().when(openOrderRepository.findByOrderId(any(Long.class))).thenReturn(Optional.of(algoTradeTestDtoFactory.getOrderByIdResult()));
        Mockito.lenient().when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(algoTradeTestDtoFactory.getOpenTradeForUserAndInstruementAndStrategy());
    }

    /**
     * This method check the Test case of scenario
     * 1. Single User With Mock order, No Existing Open Trade for the same instrument
     */
    @Test
    void testCreateAsyncMockOrderWithSingleUser() {
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertEquals(12345666L, (long) orders.get(0).getOrderId());
    }


    /**
     * This method check the Test case of scenario
     * 1. 2 Users with Mock order, No Existing Open Trade for the same instrument
     * 2.
     */
    @Test
    void testCreateAsyncMockOrderWithMultiUser() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
        assertEquals(12345666L, (long) orders.get(0).getOrderId());
    }

    @Test
    void testCreateAsyncMockSingleLotOrderWithSingleUser() {
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertEquals(12345666L, (long) orders.get(0).getOrderId());
    }

    @Test
    void testCreateAsyncMockSingleLotOrderWithMultiUser() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(15);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
        assertEquals(12345666L, (long) orders.get(0).getOrderId());
        assertTrue(orders.get(1).getOrderId() == 12345666L && "User2".equalsIgnoreCase(orders.get(1).getUserId()));
    }

    @Test
    void testCreateAsyncMockMultiLotOrderWithSingleUser() {
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(300);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertEquals(12345666L, (long) orders.get(0).getOrderId());
    }

    @Test
    void testCreateAsyncMultiLotOrderWithMultiUser() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(300);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
        assertEquals(12345666L, (long) orders.get(0).getOrderId());
        assertEquals(12345666L, (long) orders.get(1).getOrderId());

        assertEquals(orders.stream()
                .filter(o -> o.getUserId().equalsIgnoreCase(allUsers.get(0).getUserId()))
                .map(OrderResponse::getQuantity)
                .reduce(0, Integer::sum), orderRequest.getQuantity());
        assertEquals(orders.stream()
                .filter(o -> o.getUserId().equalsIgnoreCase(allUsers.get(1).getUserId()))
                .map(OrderResponse::getQuantity)
                .reduce(0, Integer::sum), orderRequest.getQuantity());
    }


    @Test
    void testCreateAsyncMockMoreLotThanBrokerLimitPerOrderWithSingleUser() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(1200);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
        assertEquals(12345666L, (long) orders.get(0).getOrderId());

/*        assertTrue(orders.stream()
                .filter(o -> o.getUserId().equalsIgnoreCase(allUsers.get(0).getUserId()))
                .map(o -> o.getQuantity())
                .reduce(0, (x, y) -> x + y).equals(orderRequest.getQuantity()));*/
    }

    @Test
    void testCreateAsyncMoreLotThanBrokerLimitPerOrderWithMultiUser() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(1200);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(4, orders.size());
        assertTrue(orders.get(0).getOrderId() == 12345666L && "User1".equalsIgnoreCase(orders.get(0).getUserId()));
        assertTrue(orders.get(1).getOrderId() == 12345666L && "User1".equalsIgnoreCase(orders.get(1).getUserId()));

/*        assertTrue(orders.stream()
                .filter(o -> o.getUserId().equalsIgnoreCase(allUsers.get(0).getUserId()))
                .map(o -> o.getQuantity())
                .reduce(0, (x, y) -> x + y).equals(orderRequest.getQuantity()));
        assertTrue(orders.stream()
                .filter(o -> o.getUserId().equalsIgnoreCase(allUsers.get(1).getUserId()))
                .map(o -> o.getQuantity())
                .reduce(0, (x, y) -> x + y).equals(orderRequest.getQuantity()));*/
    }

    @Test
    void testCreateAsyncOrderWithSingleUserOrderCreationFailed() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(1200);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenThrow(new KotakValidationException());
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertEquals(true, orders.isEmpty());
        assertEquals(0, orders.size());
    }

    @Test
    void testCreateAsyncOrderWithSingleUserOrderCreationFailedThenDeleteTrade() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(300);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenThrow(new KotakValidationException());
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        verify(tradeService, times(1)).deleteTrade(any(TradeEntity.class));
        assertEquals(true, orders.isEmpty());
        assertEquals(0, orders.size());
    }

    @Test
    void testCreateAsyncMockMultiLotOrderWithSingleUserWithExactMultipleQuantityOfNSELimit() {
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(1800);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
        assertEquals(12345666L, (long) orders.get(0).getOrderId());
    }

    @Test
    void testCreateAsyncMockSingleUserWithTokenWhichIsNotAvailableInInstrumentMaster() {
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setInstrumentToken(00000l);
        orderRequest.setInstrumentName("Invalid Token");
        orderRequest.setQuantity(15);
        AlgoValidationException algoValidationException = assertThrows(AlgoValidationException.class, () -> {
            orderService.createOrder(orderRequest);
        });
        assertTrue(1 == algoValidationException.getError().getVoilations().size());
        assertTrue(algoValidationException.getError().getVoilations().get(0).getMessage().contains("Invalid or Not found in instrument list"));
    }


    @Test
    void testCreateAsyncMockSingleUserWithNoDataInTokenMaster() {
        when(instrumentService.getAllInstruments()).thenReturn(new ArrayList<>());
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setInstrumentToken(00000l);
        orderRequest.setInstrumentName("Invalid Token");
        orderRequest.setQuantity(15);
        AlgotradeException algoValidationException = assertThrows(AlgotradeException.class, () -> {
            orderService.createOrder(orderRequest);
        });
        assertTrue(StringUtils.isNoneBlank(algoValidationException.getError().getMessage()) && ErrorCodeConstants.EMPTY_INSTRUMENTS_ERROR.equals(algoValidationException.getError()));
        assertTrue(StringUtils.isNoneBlank(algoValidationException.getError().getMessage()) && "Empty or no data found in instrument master".equals(algoValidationException.getError().getMessage()));
    }

    @Test
    void testCreateAsyncOrderWithSingleUserLiveFeedShouldNotReconnectInOffMarketHours() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(300);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        if (!CommonUtils.getOffMarketHoursTestFlag() && !DateUtils.isTradingSessionTime()) {
            verify(websocketUtils, times(0)).addAndSubscribeInstrument(any(Long.class));
        }
        assertEquals(false, orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    void testCreateAsyncOrderWithMultiUserLiveFeedShouldReconnectInTradingHours() throws DuplicateKeyException {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(300);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);

//        if (DateUtils.isTradingSessionTime()) {
//            verify(websocketUtils, times(1)).addInstrumentAndReconnectWebsocket(any(Long.class));
//        }

        assertEquals(false, orders.isEmpty());
        assertEquals(2, orders.size());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateOpenTrade() {
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(300);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithMultiUserValidateOpenTrade() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        TradeEntity tradeEntity = algoTradeTestDtoFactory.getTodaysTrade().get(0);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(300);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateSquareoffOrderForNoOpenTrade() {
//        TradeEntity tradeEntity = algoTradeTestDtoFactory.getTodaysTrade().get(0);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setOrderCategory(OrderCategoryEnum.SQAREOFF);
        orderRequest.setTriggerType(TriggerType.AUTO);
        orderRequest.setQuantity(300);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateSquareOffOrderForOpenTrade() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setOrderCategory(OrderCategoryEnum.SQAREOFF);
        orderRequest.setTriggerType(TriggerType.AUTO);
        orderRequest.setTransactionType(TransactionType.SELL);
        orderRequest.setQuantity(10);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategyAndTradeStatus(any(String.class),any(Long.class),any(String.class),any(TradeStatus.class))).thenReturn(Optional.of(tradeEntity.get(0)));
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateSquareOffOrderForOpenSellOrder() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        tradeEntity.get(0).setBuyOpenQuantityToSquareOff(15);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setOrderCategory(OrderCategoryEnum.SQAREOFF);
        orderRequest.setTriggerType(TriggerType.AUTO);
        orderRequest.setTransactionType(TransactionType.BUY);
        orderRequest.setQuantity(10);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategyAndTradeStatus(any(String.class),any(Long.class),any(String.class),any(TradeStatus.class))).thenReturn(Optional.of(tradeEntity.get(0)));
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateSquareOffOrderForOpenTradeWithInvalidQuantity() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setOrderCategory(OrderCategoryEnum.SQAREOFF);
        orderRequest.setTriggerType(TriggerType.AUTO);
        orderRequest.setTransactionType(TransactionType.SELL);
        orderRequest.setQuantity(20);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithMultipleUserValidateSquareOffOrderForOpenTrade() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setOrderCategory(OrderCategoryEnum.SQAREOFF);
        orderRequest.setTriggerType(TriggerType.AUTO);
        orderRequest.setTransactionType(TransactionType.SELL);
        orderRequest.setOrderType(OrderType.SL);
        orderRequest.setQuantity(10);
        orderRequest.setPrice(new BigDecimal(130.32));
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategyAndTradeStatus(any(String.class),any(Long.class),any(String.class),any(TradeStatus.class))).thenReturn(Optional.of(tradeEntity.get(0)));
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        when(instrumentService.getInstrumentByToken(any(Long.class))).thenReturn(algoTradeTestDtoFactory.getInstrument());
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
    }

    @Test
    void testCreateAsyncMockOrderWithMultipleUserValidateSquareOffOrderForSellOrder() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        tradeEntity.get(0).setBuyOpenQuantityToSquareOff(15);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setOrderCategory(OrderCategoryEnum.SQAREOFF);
        orderRequest.setTriggerType(TriggerType.AUTO);
        orderRequest.setTransactionType(TransactionType.BUY);
        orderRequest.setQuantity(10);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategyAndTradeStatus(any(String.class),any(Long.class),any(String.class),any(TradeStatus.class))).thenReturn(Optional.of(tradeEntity.get(0)));
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateOrderQuantityRounding() {
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(47);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(45));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertEquals(45,  orders.get(0).getQuantity());
    }

    @Test
    void testCreateAsyncMockOrderWithMultiUserValidateOrderQuantityRounding() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(47);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(45));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
        assertEquals(45,  orders.get(0).getQuantity());
        assertEquals(45,  orders.get(1).getQuantity());
    }


    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateMargin() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setPrice(new BigDecimal(100));
        orderRequest.setQuantity(20);
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        allUsers.get(0).setIsRealTradingEnabled(Boolean.TRUE);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(marginService.getMargin("User1")).thenReturn(new BigDecimal(2000));
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserInvalidMargin() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setPrice(new BigDecimal(100));
        orderRequest.setQuantity(20);
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        allUsers.get(0).setIsRealTradingEnabled(Boolean.TRUE);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(marginService.getMargin("User1")).thenReturn(new BigDecimal(1000));
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserNullMargin() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setPrice(new BigDecimal(100));
        orderRequest.setQuantity(20);
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        allUsers.get(0).setIsRealTradingEnabled(Boolean.TRUE);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(marginService.getMargin("User1")).thenReturn(BigDecimal.ZERO);
        doNothing().when(marginService).updateMarginInDB(any(String.class), any(BigDecimal.class));
        when(marginService.getMarginFromKotak("User1")).thenReturn(algoTradeTestDtoFactory.getMarginData());
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserNullMarginFromKotak() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setPrice(new BigDecimal(100));
        orderRequest.setQuantity(20);
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        allUsers.get(0).setIsRealTradingEnabled(Boolean.TRUE);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(marginService.getMargin("User1")).thenReturn(BigDecimal.ZERO);
        when(marginService.getMarginFromKotak("User1")).thenReturn(null);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithMultiUserValidateMargin() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("User2");
        allUsers.add(user2);
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setPrice(new BigDecimal(100));
        orderRequest.setQuantity(20);
        allUsers.stream().forEach(userResponse -> userResponse.setIsRealTradingEnabled(Boolean.TRUE));
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(marginService.getMargin("User1")).thenReturn(new BigDecimal(2000));
        when(marginService.getMargin("User2")).thenReturn(new BigDecimal(2000));
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
    }


    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateMaxTradePerDay() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        TradeEntity tradeEntity1 = TradeEntity.builder().id("trade2").tradeStatus(TradeStatus.COMPLETED).build();
        tradeEntity.add(tradeEntity1);
        TradeEntity tradeEntity2 = TradeEntity.builder().id("trade3").tradeStatus(TradeStatus.COMPLETED).build();
        tradeEntity.add(tradeEntity2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithMultiUserValidateMaxTradePerDay() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("PO1131");
        Map<String, Integer> map =  new HashMap<>();
        map.put("FNO", 3);
        user2.setMaxTradesPerDay(map);
        allUsers.add(user2);
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        TradeEntity tradeEntity1 = TradeEntity.builder().id("trade2").tradeStatus(TradeStatus.COMPLETED).build();
        tradeEntity.add(tradeEntity1);
        TradeEntity tradeEntity2 = TradeEntity.builder().id("trade3").tradeStatus(TradeStatus.COMPLETED).build();
        tradeEntity.add(tradeEntity2);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateMaxLossPerDay() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTodaysRealisedPnl(any(String.class))).thenReturn(new BigDecimal(-11000));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncOrderWithSingleUserValidateMaxLossPerDay() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        orderRequest.setPrice(new BigDecimal(100));
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        tradeEntity.get(0).setRealisedPnl(new BigDecimal(2000));
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        allUsers.get(0).setIsRealTradingEnabled(Boolean.TRUE);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(kotakClient.getTodaysOpenPostions(any(String.class))).thenReturn(algoTradeTestDtoFactory.getTodaysPositionResponse());
        when(marginService.getMargin(any(String.class))).thenReturn(new BigDecimal(5000));
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    void testCreateAsyncOrderWithSingleUserValidateMaxLossPerDayForLoss() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        orderRequest.setPrice(new BigDecimal(100));
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        tradeEntity.get(0).setRealisedPnl(new BigDecimal(-9000));
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        allUsers.get(0).setIsRealTradingEnabled(Boolean.TRUE);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(kotakClient.getTodaysOpenPostions(any(String.class))).thenReturn(algoTradeTestDtoFactory.getTodaysPositionResponse());
        when(marginService.getMargin(any(String.class))).thenReturn(new BigDecimal(5000));
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    void testCreateAsyncOrderWithSingleUserValidateMaxLossPerDayForLossMoreThanLimit() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        orderRequest.setPrice(new BigDecimal(100));
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        tradeEntity.get(0).setRealisedPnl(new BigDecimal(-11000));
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        allUsers.get(0).setIsRealTradingEnabled(Boolean.TRUE);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(kotakClient.getTodaysOpenPostions(any(String.class))).thenReturn(algoTradeTestDtoFactory.getTodaysPositionResponse());
        when(marginService.getMargin(any(String.class))).thenReturn(new BigDecimal(5000));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithMultiUserValidateMaxLossPerDay() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("PO1131");
        Map<String, BigDecimal> map =  new HashMap<>();
        map.put("FNO", new BigDecimal(-10000));
        user2.setDayLossLimit(map);
        allUsers.add(user2);
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTodaysRealisedPnl(any(String.class))).thenReturn(new BigDecimal(-11000));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateProfitPerDay() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        tradeEntity.get(0).setRealisedPnl(new BigDecimal(11000));
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTodaysRealisedPnl(any(String.class))).thenReturn(new BigDecimal(11000));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithMultiUserValidateProfitPerDay() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("PO1131");
        Map<String, BigDecimal> map =  new HashMap<>();
        map.put("FNO", new BigDecimal(-10000));
        user2.setDayLossLimit(map);
        Map<String, BigDecimal> profitMap =  new HashMap<>();
        profitMap.put("FNO", new BigDecimal(10000));
        user2.setDayProfitLimit(profitMap);
        allUsers.add(user2);
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        tradeEntity.get(0).setRealisedPnl(new BigDecimal(11000));
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTodaysRealisedPnl(any(String.class))).thenReturn(new BigDecimal(11000));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserValidateExistingOpenTrade() {
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithMultiUserValidateExistingOpenTrade() {
        List<UserResponse> allUsers = algoTradeTestDtoFactory.getAllUsers();
        UserResponse user2 = algoTradeTestDtoFactory.createUser();
        user2.setUserId("PO1131");
        allUsers.add(user2);
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(allUsers);
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testCreateAsyncMockOrderWithAllPreValidationChecks(){
        List<TradeEntity> tradeEntity = algoTradeTestDtoFactory.getTodaysTrade();
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setQuantity(10);
        orderRequest.setSegment("FNO");
        tradeEntity.get(0).setRealisedPnl(new BigDecimal(9000));
        tradeEntity.get(0).setTradeStatus(TradeStatus.COMPLETED);
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        when(tradeService.getTodaysTradeByUserId(any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTradeByUserIdAndInstrumentTokenAndStrategy(any(String.class), any(Long.class), any(String.class))).thenReturn(tradeEntity);
        when(tradeService.getTodaysRealisedPnl(any(String.class))).thenReturn(new BigDecimal(9000));
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    void testCreateAsyncMockOrderWithSingleUserExpiryStrategy() {
        when(userService.getUsersbyIds(any(List.class))).thenReturn(algoTradeTestDtoFactory.getAllUsers());
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setStrategyName(Constants.STRATEGY_EXPIRY);
        orderRequest.setQuantity(null);
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(15));
        when(userStrategyService.findStrategyByUserIdAndStrategy(any(String.class), any(String.class))).thenReturn(UserStrategyEntity.builder().build());
        StrategyResponse strategyResponse = new StrategyResponse();
        EntryCondition entryCondition = new EntryCondition();
        Map<String, String> conditions = new HashMap<String, String>();
        conditions.put(Constants.LAST_ORDER_QUANTITY, "450");
        conditions.put(Constants.DEFAULT_QUANTITY, "15");
        entryCondition.setConditions(conditions);
        strategyResponse.setEntryCondition(entryCondition);
        when(strategyService.getStrategy(any(String.class))).thenReturn(strategyResponse);
        List<OrderResponse> orders = orderService.createOrder(orderRequest);
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertEquals(12345666L, (long) orders.get(0).getOrderId());
    }

    @Test
    void testModifyOrderWithNoOrdersToModify(){
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        when(openOrderRepository
                .findByOrderIdAndOrderStatusAndCreatedAtGreaterThanEqual(any(Long.class),
                        any(OrderStatus.class), any(LocalDateTime.class))).thenReturn(Optional.empty());
        List<OrderResponse> orderResponses = orderService.modifyOrder(1L, orderRequest, null, false);
        assertTrue(orderResponses.isEmpty());
    }

    @Test
    void testModifyOrderWithModificationCountLessThanConfiguredCount(){
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setPrice(new BigDecimal(100));
        orderRequest.getSlDetails().setTriggerPrice(new BigDecimal(100));
        OpenOrderEntity openOrderEntity = algoTradeTestDtoFactory.getOrderByIdResult();
        openOrderEntity.setModificationCount(0);
        UserResponse userResponse = algoTradeTestDtoFactory.createUser();
        when(openOrderRepository
                .findByOrderIdAndOrderStatusAndCreatedAtGreaterThanEqual(any(Long.class),
                        any(OrderStatus.class), any(LocalDateTime.class))).thenReturn(Optional.of(openOrderEntity));
        when(userService.getUserById(openOrderEntity.getUserId())).thenReturn(userResponse);
        when(algoConfigurationService.getConfigurationDetails()).thenReturn(algoTradeTestDtoFactory.getConfigurationDetails());
        when(kotakClient.modifyOrder(any(KotakOrderRequest.class), any(String.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(15));
        when(openOrderRepository.save(openOrderEntity)).thenReturn(openOrderEntity);
        List<OrderResponse> orderResponses = orderService.modifyOrder(1L, orderRequest, null, false);
        assertFalse(orderResponses.isEmpty());
        assertEquals(1, orderResponses.size());
    }

    @Test
    void testModifyOrderWithModificationCountMoreThanConfiguredCount(){
        OrderRequest orderRequest = algoTradeTestDtoFactory.getOrderRequest();
        orderRequest.setPrice(new BigDecimal(100));
        orderRequest.getSlDetails().setTriggerPrice(new BigDecimal(100));
        OpenOrderEntity openOrderEntity = algoTradeTestDtoFactory.getOrderByIdResult();
        openOrderEntity.setModificationCount(9);
        UserResponse userResponse = algoTradeTestDtoFactory.createUser();
        when(openOrderRepository
                .findByOrderIdAndOrderStatusAndCreatedAtGreaterThanEqual(any(Long.class),
                        any(OrderStatus.class), any(LocalDateTime.class))).thenReturn(Optional.of(openOrderEntity));
        when(userService.getUserById(openOrderEntity.getUserId())).thenReturn(userResponse);
        when(algoConfigurationService.getConfigurationDetails()).thenReturn(algoTradeTestDtoFactory.getConfigurationDetails());
        when(openOrderRepository.save(openOrderEntity)).thenReturn(openOrderEntity);
        when(kotakClient.cancelOrder(any(Long.class), any(String.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(15));
        when(kotakClient.placeOrder(any(KotakOrderRequest.class), any(UserResponse.class))).thenReturn(algoTradeTestDtoFactory.getPlacedOrderResponse(orderRequest.getQuantity()));
        when(tradeService.getTradeForOrder(any(String.class))).thenReturn(Optional.of(algoTradeTestDtoFactory.getTodaysTrade().get(0)));
        List<OrderResponse> orderResponses = orderService.modifyOrder(1L, orderRequest, null, false);
        verify(kotakClient, times(1)).cancelOrder(any(Long.class), any(String.class));
        verify(kotakClient, times(1)).placeOrder(any(KotakOrderRequest.class), any(UserResponse.class));
    }

}
