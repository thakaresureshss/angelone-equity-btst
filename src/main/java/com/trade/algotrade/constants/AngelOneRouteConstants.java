package com.trade.algotrade.constants;

import org.springframework.stereotype.Component;

@Component
public class AngelOneRouteConstants {
    public static final String LOGIN_BY_USERPASSWROD_URL = "/auth/angelbroking/user/v1/loginByPassword";
    public static final String RENEW_ACCESS_URL = "/auth/angelbroking/jwt/v1/generateTokens";

    public static final String GET_USER_MARGIN = "/secure/angelbroking/user/v1/getRMS";

    public static final String LOGOUT_USER = "/secure/angelbroking/user/v1/logout";

    public static final String GET_CANDLE_DATA = "/secure/angelbroking/historical/v1/getCandleData";

    public static final String GET_QUOTE_URL = "/secure/angelbroking/market/v1/quote/";

    public static final String PLACE_ORDER_URL = "/secure/angelbroking/order/v1/placeOrder";

    public static final String MODIFY_ORDER_URL = "/secure/angelbroking/order/v1/modifyOrder";

    public static final String CANCEL_ORDER_URL = "/secure/angelbroking/order/v1/cancelOrder";

    public static final String GET_ORDERS_URL = "/secure/angelbroking/order/v1/getOrderBook";
}
