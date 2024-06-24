package com.trade.algotrade.constants;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.trade.algotrade.exceptions.BusinessError;

@Component
public class ErrorCodeConstants {

    // KOTAK ORDERS will be from 2000-3000
    public static final BusinessError KOTAK_CREATE_ORDER_ERROR = BusinessError.builder().code("2001")
            .message("Empty or null result recieved in Kotak create order").build();

    public static final BusinessError KOTAK_GET_ORDER_ERROR = BusinessError.builder().code("2002")
            .message("Empty or null result recieved in Kotak get order").build();

    public static final BusinessError INVALID_ACCESS_TOKEN = BusinessError.builder().code("2003").message("Invalid Access toekn Data")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    // KOTAK ORDERS will be from 1000-2000
    public static final BusinessError EMPTY_OR_NULL_TOKEN_ERROR = BusinessError.builder().code("1001")
            .message("Empty or null kotak trade api token found").build();

    public static final BusinessError EMPTY_NULL_OR_SESSION_TOKEN_ERROR = BusinessError.builder().code("1002")
            .message("Empty or null kotak session token found").build();

    public static final BusinessError EMPTY_INSTRUMENTS_ERROR = BusinessError.builder().code("1003")
            .code(HttpStatus.BAD_REQUEST.toString()).message("Empty or no data found in instrument master").build();

    public static final BusinessError EMPTY_CONFIG_ERROR = BusinessError.builder().code("1004")
            .code(HttpStatus.BAD_REQUEST.toString()).message("No application config defined, Please define Required configurations").build();

    public static final BusinessError NO_ACTIVE_USER_ERROR = BusinessError.builder().code("1005")
            .message("Empty or No active users found or strategy is not enabled.").status(HttpStatus.BAD_REQUEST)
            .build();

    public static final BusinessError ENTRY_CONDITION_NOT_FOUND = BusinessError.builder().code("1006")
            .message("Entry condition not found.").status(HttpStatus.BAD_REQUEST).build();

    public static final BusinessError DUPLICATE_STRATEGY_FOUND = BusinessError.builder().code("1007")
            .message("Duplicate strategy found.").status(HttpStatus.BAD_REQUEST).build();

    public static final BusinessError STRATEGY_NOT_FOUND = BusinessError.builder().code("1008")
            .message("No strategy found in system with provided name").status(HttpStatus.BAD_REQUEST).build();

    public static final BusinessError STOCK_MASTER_NOT_FOUND = BusinessError.builder().code("1009")
            .message("No stock available for specified name").status(HttpStatus.BAD_REQUEST).build();

    public static final BusinessError DUPLICATE_STOCK_MASTER = BusinessError.builder().code("1010")
            .message("Duplicate stock found with name").status(HttpStatus.BAD_REQUEST).build();

    public static final BusinessError EMPTY_NULL_OR_SESSION_WEBSOCKET_TOKEN_ERROR = BusinessError.builder().code("1011")
            .message("Empty or null kotak websocket auth token found").build();

    public static final BusinessError NOT_FOUND_DEFAULT_QUANTITY = BusinessError.builder().code("1012")
            .status(HttpStatus.BAD_REQUEST).message("Default Quantity Not Found").build();

    public static final BusinessError NOT_STOP_LOSS_POINTS_FOUND = BusinessError.builder().code("1013")
            .status(HttpStatus.BAD_REQUEST).message("stop loss points not defined").build();

    public static final BusinessError ORDER_NOT_FOUND_ERROR = BusinessError.builder().code("1014")
            .status(HttpStatus.BAD_REQUEST).message("Invalid order id or not found in database").build();

    public static final BusinessError TRADE_QUANTITY_NOTHING_TO_UPDATE = BusinessError.builder().code("1008")
            .message("Nothing to update for trade quantity").status(HttpStatus.BAD_REQUEST).build();

    public static final BusinessError KOTAK_CREATE_ORDER_FAILED = BusinessError.builder().code("2003")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_MDODIFY_ORDER_FAILED = BusinessError.builder().code("2004")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_MARKET_DEPTH_FAILED = BusinessError.builder().code("2005")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();


    public static final BusinessError CONFIG_MISSING_FOR_KEY = BusinessError.builder().code("1014")
            .code(HttpStatus.BAD_REQUEST.toString()).message("Configuration is not defined for the provided config key").build();


    public static final BusinessError INVALID_ENUM_VALUES = BusinessError.builder().code("1014")
            .code(HttpStatus.BAD_REQUEST.toString()).message("Invalid Possible Values").build();
    public static final BusinessError KOTAK_WEBSOCKET_ACCESS_TOKEN_FAILED = BusinessError.builder().code("2006")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_INSTRUMENT_DETAILS_FAILED = BusinessError.builder().code("2007")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_OTT_TOKEN_FAILED = BusinessError.builder().code("2008")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_SESSION_TOKEN_FAILED = BusinessError.builder().code("2009")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_TADAYS_POSITION_FAILED = BusinessError.builder().code("2010")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_ALL_OPEN_POSITION_FAILED = BusinessError.builder().code("2011")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_GET_ORDER_FAILED = BusinessError.builder().code("2012")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_GET_ALL_ORDER_FAILED = BusinessError.builder().code("2013")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_LTP_FAILED = BusinessError.builder().code("2005")
            .message("Exception occurred while getting ltp").status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError USER_PASSWORD_DECRYPTION_FAILED = BusinessError.builder().code("2014")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError USER_ID_ALREADY_EXIST_FAILED = BusinessError.builder().code("2015")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).message("User id is already exists").build();

    public static final BusinessError USER_NOT_EXIST_FAILED = BusinessError.builder().code("2015")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).message("User is not exists").build();

    public static final BusinessError KOTAK_CANCLE_ORDER_FAILED = BusinessError.builder().code("2016")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_GET_MARGIN_FAILED = BusinessError.builder().code("2017")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError KOTAK_ANGEL_INSTRUMENT_MAPPING_NOT_FOUND = BusinessError.builder().code("2018")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).message("Kotak adn Angel instrument mapping not found").build();


    public static final BusinessError OPEN_STRATEGY_ORDER_FOUND = BusinessError.builder().code("2011").message("Open Orders exists for this strategy")
            .status(HttpStatus.BAD_REQUEST).build();




    public static final BusinessError WEB_SOCKET_RECONNECTION_FAILED = BusinessError.builder().code("4001")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).message("Websocket reconnection failed").build();

    public static final BusinessError NOT_FOUND_NEWS_SPIKE_PROFIT_PERCENT = BusinessError.builder().code("1016")
            .status(HttpStatus.BAD_REQUEST).message("News Spike Equity Strategy Configuration missing for Profit Percent").build();
    public static final BusinessError NOT_FOUND_NEWS_SPIKE_LOSS_PERCENT = BusinessError.builder().code("1017")
            .status(HttpStatus.BAD_REQUEST).message("News Spike Equity Strategy Configuration missing for Loss Percent").build();

    public static final BusinessError EXIT_CONDITION_MISSING = BusinessError.builder().code("1004")
            .code(HttpStatus.BAD_REQUEST.toString()).message("Exit condition configs are missing").build();




}
