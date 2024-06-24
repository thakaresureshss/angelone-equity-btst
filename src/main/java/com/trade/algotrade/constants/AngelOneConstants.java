package com.trade.algotrade.constants;

import org.springframework.stereotype.Component;

@Component
public class AngelOneConstants {
    public static final Integer MAX_RETRY_ACCESS_TOKEN = 3;

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_HEADER_BEARER = "Bearer ";


    public static final String NFO = "NFO";

    public static final Integer MAX_RETRY_CANDLE_DATA = 2;
}
