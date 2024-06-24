package com.trade.algotrade.constants;

import com.trade.algotrade.exceptions.BusinessError;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AngelOneErrorCodeConstants {

    public static final BusinessError GET_ACCESS_TOKEN_FAILED = BusinessError.builder().code("5008")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError LOGOUT_USER_FAILED = BusinessError.builder().code("5009")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError GET_CANDLE_DATA_FAILED = BusinessError.builder().code("5010")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();


    public static final BusinessError GET_ACCESS_TOKEN_EMPTY = BusinessError.builder().code("5008")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError GET_QUOTE_ERROR = BusinessError.builder().code("5008")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    public static final BusinessError GET_ORDERS_ERROR = BusinessError.builder().code("5009")
            .status(HttpStatus.INTERNAL_SERVER_ERROR).build();
}
