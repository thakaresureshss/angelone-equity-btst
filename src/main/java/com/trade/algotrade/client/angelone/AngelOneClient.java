package com.trade.algotrade.client.angelone;

import com.trade.algotrade.client.angelone.enums.ExchangeType;
import com.trade.algotrade.client.angelone.request.GetCandleRequest;
import com.trade.algotrade.client.angelone.request.feed.GetLtpRequest;
import com.trade.algotrade.client.angelone.request.orders.AngelAllOrdersResponse;
import com.trade.algotrade.client.angelone.request.orders.AngelOrderRequest;
import com.trade.algotrade.client.angelone.request.orders.AngelOrderResponse;
import com.trade.algotrade.client.angelone.response.feed.QouteResponse;
import com.trade.algotrade.client.angelone.response.historical.GetCandleResponse;
import com.trade.algotrade.client.angelone.response.user.*;
import com.trade.algotrade.response.UserResponse;

import java.util.List;

public interface AngelOneClient {

    GetAccessTokenResponse getAccessToken(UserResponse userResponse);

    List getScripDetails();

    List<GetAccessTokenResponse> getAccessTokenForAllActiveUsers();

    RenewAccessTokenResponse renewAccessToken(String refreshToken, UserResponse userResponse);

    GetMarginResponse getMargin(String currentAccessToken, UserResponse userResponse);

    GetUserProfileResponse getUserProfile();

    LogoutUserResponse logoutUser(String currentAccessToken, UserResponse userResponse);

    void logoutAllActiveUsers();

    GetCandleResponse getCandleData(String currentAccessToken, UserResponse userResponse, GetCandleRequest getCandleRequest);

    QouteResponse getCandleData(String currentAccessToken, UserResponse userResponse, GetLtpRequest getLtpRequest);

    GetAccessTokenResponse getAccessTokenSystemUser();

    QouteResponse getQuote(ExchangeType exchangeType, List<String> instruments);

    AngelOrderResponse placeOrder(AngelOrderRequest angelOrderRequest, UserResponse userDetails);

    AngelOrderResponse modifyOrder(AngelOrderRequest angelOrderRequest, String userId);

    AngelOrderResponse cancelOrder(Long orderId, String userId);

    AngelAllOrdersResponse getAllOrders(String userId);

}
