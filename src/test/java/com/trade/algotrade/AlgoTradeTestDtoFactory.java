package com.trade.algotrade;

import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.client.angelone.response.user.MarginData;
import com.trade.algotrade.client.kotak.dto.CreateOrderDto;
import com.trade.algotrade.client.kotak.dto.MarginCoreDto;
import com.trade.algotrade.client.kotak.dto.MarginDto;
import com.trade.algotrade.client.kotak.dto.TodaysPositionSuccess;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.client.kotak.response.KotakOrderResponse;
import com.trade.algotrade.client.kotak.response.TodaysPositionResponse;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.dto.ConfigurationDetails;
import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enitiy.TradeEntity;
import com.trade.algotrade.enitiy.UserEntity;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.request.UserRequest;
import com.trade.algotrade.response.ConfigurationResponse;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.utils.DateUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class AlgoTradeTestDtoFactory {

    public List<AngelOneInstrumentMasterEntity> getAllInstruments() {
        return List.of(AngelOneInstrumentMasterEntity.builder().instrumentToken(11717L)
                        .lotSize(15)
                        .strike(43900)
                        .optionType("PE")
                        .instrumentName("NIFTY BANK")
                        .build(),
                AngelOneInstrumentMasterEntity.builder()
                        .instrumentToken(29126L)
                        .strike(43900)
                        .lotSize(15)
                        .optionType("CE")
                        .instrumentName("NIFTY BANK")
                        .build(),
                AngelOneInstrumentMasterEntity.builder()
                        .instrumentToken(29126L)
                        .strike(44000)
                        .lotSize(15)
                        .optionType("PE")
                        .instrumentName("NIFTY BANK")
                        .build(),
                AngelOneInstrumentMasterEntity.builder()
                        .instrumentToken(29126L)
                        .strike(43800)
                        .lotSize(15)
                        .optionType("CE")
                        .instrumentName("NIFTY BANK")
                        .build()
        );
    }

    public OrderRequest getOrderRequest() {
        OrderRequest orderRequest = new OrderRequest();
        SlDetails slDetails = new SlDetails();
        slDetails.setSlPoints(new BigDecimal(30));
        slDetails.setTrailSl(true);
        slDetails.setSlBufferPoints(new BigDecimal(10));
        slDetails.setSlPrice(new BigDecimal(100.32));
        orderRequest.setSlDetails(slDetails);
        orderRequest.setQuantity(15);
        orderRequest.setInstrumentToken(29126L);
        orderRequest.setUserId("User2");
        orderRequest.setTransactionType(TransactionType.BUY);
        orderRequest.setOptionType(OptionType.CE);
        orderRequest.setProduct(com.trade.algotrade.client.angelone.enums.ProductType.INTRADAY);
//        orderRequest.setValidity(ValidityEnum.GFD);
        orderRequest.setVariety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL);
        orderRequest.setStrategyName(Strategy.BIGCANDLE.toString());
        orderRequest.setOrderCategory(OrderCategoryEnum.NEW);
        orderRequest.setOrderType(OrderType.MARKET);
        orderRequest.setTag(Constants.ALGOTRADE_TAG);
        return orderRequest;
    }


    public List<UserResponse> getAllUsers() {
        List<UserResponse> userResponses = new ArrayList<>();
        UserResponse user1 = createUser();
        user1.setUserId("User1");
        userResponses.add(user1);

/*
        UserResponse user2 = createUser();
        user2.setUserId("User2");
        userResponses.add(user2);
*/

        return userResponses;
    }

    public UserResponse createUser() {
        UserResponse userResponse = new UserResponse();
        userResponse.setUserId("User1");
        userResponse.setId(1l);
        userResponse.setPassword("dummyPassword");
        userResponse.setBroker("Kotak");
        userResponse.setAppId("AlgoTrade");
        userResponse.setConsumerKey("Dummy Consumer key");
        userResponse.setConsumerSecrete("Dummy Consumer secrete ");
        userResponse.setExchange("NSE");
        userResponse.setActive(true);
        Map<String, Integer> map = new HashMap<>();
        map.put("FNO", 2);
        userResponse.setMinTradesPerDay(map);
        userResponse.setMaxTradesPerDay(map);
        Map<String, BigDecimal> lossMap =  new HashMap<>();
        lossMap.put("FNO", new BigDecimal(10000));
        userResponse.setDayLossLimit(lossMap);
        Map<String, BigDecimal> profitMap =  new HashMap<>();
        profitMap.put("FNO", new BigDecimal(10000));
        userResponse.setDayProfitLimit(profitMap);
        return userResponse;
    }

    public List<TradeEntity> getOpenTradeForUserAndInstruementAndStrategy() {
        List<TradeEntity> tradeEntities = new ArrayList<>();
        TradeEntity tradeEntity = getTrade();
        tradeEntity.setTradeStatus(TradeStatus.OPEN);
        tradeEntity.setSellOpenQuantityToSquareOff(15);
        tradeEntity.setBuyOpenQuantityToSquareOff(null);
        tradeEntity.setRealisedPnl(null);
        return tradeEntities;
    }

    public List<TradeEntity> getTodaysTrade() {
        List<TradeEntity> tradeEntities = new ArrayList<>();
        TradeEntity tradeEntity = getTrade();
        tradeEntity.setTradeStatus(TradeStatus.OPEN);
        tradeEntity.setSellOpenQuantityToSquareOff(15);
        tradeEntity.setBuyOpenQuantityToSquareOff(null);
        tradeEntity.setRealisedPnl(null);
        tradeEntity.setCreatedTime(LocalDateTime.now());
        tradeEntity.setUpdatedTime(LocalDateTime.now());
        tradeEntity.setOrders(new ArrayList<>());
        tradeEntities.add(tradeEntity);
        return tradeEntities;
    }

    private static TradeEntity getTrade() {
        TradeEntity tradeEntity = TradeEntity.builder().build();
        tradeEntity.setUserId("User1");
        tradeEntity.setTradeId(UUID.randomUUID().toString());
        tradeEntity.setStrategy(Strategy.BIGCANDLE.toString());
        tradeEntity.setOrders(new ArrayList<>());
        tradeEntity.setInstrumentToken(29126L);
        return tradeEntity;
    }

    public BigDecimal getUserMargin() {
        return new BigDecimal(20000);
    }

    public List<OpenOrderEntity> getTodaysEmptyOpenOrders() {
        return new ArrayList<>();
    }


    public List<OpenOrderEntity> getTodaysOpenOrders() {
        List<OpenOrderEntity> openOrders = new ArrayList<>();
        SlDetails slDetails = new SlDetails();
        slDetails.setTriggerPrice(new BigDecimal(64));
        OpenOrderEntity marketOrder = OpenOrderEntity.builder()
                .userId("User1")
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
                .strategyName(Strategy.BIGCANDLE.toString())
                .SlDetailsHistory(Collections.EMPTY_LIST)
                .build();
        openOrders.add(marketOrder);


        OpenOrderEntity limitOrder = OpenOrderEntity.builder()
                .userId("User1")
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
                .SlDetailsHistory(Collections.EMPTY_LIST)
                .build();
        //openOrders.add(limitOrder);


        OpenOrderEntity marketOrderUser2 = OpenOrderEntity.builder()
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
                .build();
        // openOrders.add(marketOrderUser2);
        OpenOrderEntity limitOrderUser2 = OpenOrderEntity.builder()
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
                .build();
        // openOrders.add(marketOrderUser2);
        return openOrders;
    }

    public List<OpenOrderEntity> getTodaysOpenOrdersByUserId() {
        List<OpenOrderEntity> openOrders = new ArrayList<>();
        SlDetails slDetails = new SlDetails();
        slDetails.setSlPrice(new BigDecimal(79));
        slDetails.setTriggerPrice(new BigDecimal(64));
        OpenOrderEntity marketOrder = OpenOrderEntity.builder()
                .userId("User1")
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
                .SlDetailsHistory(Collections.EMPTY_LIST)
                .build();
        openOrders.add(marketOrder);
        OpenOrderEntity limitOrder = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(25)
                .price(new BigDecimal(80))
                .validity(ValidityEnum.GFD)
                .variety(com.trade.algotrade.client.angelone.enums.VarietyEnum.NORMAL)
                .slDetails(slDetails)
                .instrumentToken(29126L)
                .orderId(2230406090644L)
                .optionType(OptionType.CE)
                .strikePrice(41100)
                .orderStatus(OrderStatus.EXECUTED)
                .strategyName(Strategy.BIGCANDLE.toString())
                .SlDetailsHistory(Collections.EMPTY_LIST)
                .build();
       // openOrders.add(limitOrder);
        return openOrders;
    }


    public List<OpenOrderEntity> getTodaysSLOrder() {
        List<OpenOrderEntity> slOrders = new ArrayList<>();
        SlDetails slDetails = new SlDetails();
        slDetails.setSlPrice(new BigDecimal(79));
        slDetails.setTriggerPrice(new BigDecimal(64));
        OpenOrderEntity slOrder = OpenOrderEntity.builder()
                .userId("User1")
                .transactionType(TransactionType.SELL)
                .orderType(OrderType.SL)
                .quantity(25)
                .price(new BigDecimal(60))
                .validity(ValidityEnum.GFD)
                .variety(VarietyEnum.NORMAL)
                .slDetails(slDetails)
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
        slOrders.add(slOrder);
        return slOrders;
    }

    public KotakOrderResponse getPlacedOrderResponse(int inputQauntity) {
        long number = (long) Math.floor(Math.random() * 9000000000000L) + 1000000000000L;
        Map<String, CreateOrderDto> map = new HashMap<>();
        CreateOrderDto createOrderDto = new CreateOrderDto();
        createOrderDto.setMessage("Your mock order has been Placed and Forwarded to the Exchange:" + number);
        createOrderDto.setOrderId(12345666l);
        createOrderDto.setPrice(new BigDecimal(120));
        createOrderDto.setQuantity(inputQauntity);
        createOrderDto.setTag("MOCKED");
        map.put("NSE", createOrderDto);
        KotakOrderResponse body = new KotakOrderResponse();
        body.setSuccess(map);
        return body;

    }


    public TradeEntity createNewTradeResponse() {
        TradeEntity tradeOrderEntity = TradeEntity.builder().build();
        tradeOrderEntity.setTradeStatus(TradeStatus.OPEN);
        tradeOrderEntity.setInstrumentToken(29126L);
        tradeOrderEntity.setUserId("User1");
        tradeOrderEntity.setCreatedTime(LocalDateTime.now());
        tradeOrderEntity.setStrategy("BigCandle");
        tradeOrderEntity.setTradeId(UUID.randomUUID().toString());
        return tradeOrderEntity;
    }

    public OpenOrderEntity getOrderByIdResult() {
        SlDetails slDetails = SlDetails.builder().slBufferPoints(new BigDecimal(10)).build();
        return OpenOrderEntity.builder().id("64e8410a1a9b9579762c1a24").orderId(12345666l).slDetails(slDetails).SlDetailsHistory(new ArrayList<>()).build();
    }

    public Optional<UserEntity> getOptionalUserEntity() {
        return Optional.empty();
    }

    public UserEntity getSavedEntity() {
        return UserEntity.builder()
                .consumerKey("SavedConsumerKey")
                .consumerSecrete("DummyConsumerSecrete")
                .broker("KOTAK")
                .active(true)
                .isRealTradingEnabled(false)
                .exchange("NSE")
                .build();
    }

    public UserRequest getUserRequest() {
        UserRequest userRequest = new UserRequest();
        userRequest.setActive(true);
        userRequest.setIsRealTradingEnabled(false);
        userRequest.setBroker("KOTAK");
        userRequest.setConsumerKey("DummyConsumerKey");
        userRequest.setConsumerSecrete("DummyConsumerSecrete");
        userRequest.setExchange("NSE");
        userRequest.setUserId("User1");
        return userRequest;
    }

    public UserResponse getUserResponse() {
        UserResponse userResponse = new UserResponse();
        userResponse.setActive(true);
        userResponse.setIsRealTradingEnabled(false);
        userResponse.setBroker("KOTAK");
        userResponse.setConsumerKey("DummyConsumerKey");
        userResponse.setConsumerSecrete("DummyConsumerSecrete");
        userResponse.setExchange("NSE");
        userResponse.setUserId("User1");
        return userResponse;
    }

    public MarginDto getMarginDto() {
        MarginDto marginDto = new MarginDto();
        MarginCoreDto marginCoreDto = new MarginCoreDto();
        marginCoreDto.setMarginAvailable(new BigDecimal(2500));
        marginDto.setEquity(marginCoreDto);
        return marginDto;
    }

    public MarginData getMarginData() {
        MarginData marginDto = new MarginData();
        marginDto.setNet("100");
        return marginDto;
    }


    public List<UserResponse> getAllActiveUsers() {
        List<UserResponse> userResponses = new ArrayList<>();
        UserResponse user1 = createUser();
        user1.setUserId("User1");
        user1.setActive(true);
        userResponses.add(user1);
        return userResponses;
    }

    public List<UserResponse> getAllInActiveUsers() {
        List<UserResponse> userResponses = new ArrayList<>();
        UserResponse user1 = createUser();
        user1.setUserId("User1");
        user1.setActive(false);
        userResponses.add(user1);
        return userResponses;
    }

    public ConfigurationResponse getConfigurationDetails(){
        ConfigurationResponse configurationResponse = new ConfigurationResponse();
        ConfigurationDetails configurationDetails = new ConfigurationDetails();
        Map<String, String> map = new HashMap<>();
        map.put("BROKER_MAX_MODIFICATION_COUNT", "9");
        configurationDetails.setConfigs(map);
        configurationResponse.setConfigurationDetails(configurationDetails);
        return configurationResponse;
    }

    public TodaysPositionResponse getTodaysPositionResponse(){
        TodaysPositionSuccess todaysPositionSuccess = new TodaysPositionSuccess();
        todaysPositionSuccess.setRealizedPL(new BigDecimal(9000));
        TodaysPositionResponse todaysPositionResponse = new TodaysPositionResponse();
        todaysPositionResponse.setSuccess(Collections.singletonList(todaysPositionSuccess));
        return todaysPositionResponse;
    }

    public AngelOneInstrumentMasterEntity getInstrument(){
        return AngelOneInstrumentMasterEntity.builder().instrumentToken(11717L)
                .lotSize(15)
                .strike(43900)
                .optionType("PE")
                .instrumentName("NIFTY BANK")
                .tickSize(new BigDecimal("0.050000"))
                .build();
    }
}
