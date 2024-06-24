package com.trade.algotrade.client.kotak;

import com.trade.algotrade.client.kotak.request.KotakOrderRequest;
import com.trade.algotrade.client.kotak.response.*;
import com.trade.algotrade.client.kotak.response.HoldingPositionResponse;
import com.trade.algotrade.client.kotak.response.TodaysPositionResponse;
import com.trade.algotrade.response.UserResponse;

import java.util.List;
import java.util.Map;

public interface KotakClient {

//	OneTimeTokenResponse getOneTimeToken();

//	TwoFactorAuthResponse getTwoFactorAuth(String oneTimeToken);

	ScripResponse getScripDetails();

	LtpResponse getLtp(String instruementToken);

	WebsocketAccessTokenResponse generateWebSocketAccessToken();

	List<WebsocketAccessTokenResponse> getAllActiveUserOrderWebsocketToken();

	// No Tested
	MarketDepthResponse getMarektDepth(long instruementToken);

	KotakOrderResponse placeOrder(KotakOrderRequest order, UserResponse userDetails);

	KotakOrderResponse modifyOrder(KotakOrderRequest order, String userId);

	KotakOrderResponse cancelOrder(Long orderId, String userId);

	KotakGetAllOrderResponse getAllOrders(String userId);

	TodaysPositionResponse getTodaysOpenPostions(String userId);

	OpenPositionsResponse getAllOpenPositions(String userId);

	HoldingPositionResponse getSoldHoldingPositions(String userId);

	Map<String, String> getSessionToken(boolean isExpired);

	KotakGetOrderResponse getOrderDetailsById(Long orderId, String userId, boolean isFnoOrder);

	MarginResponse getMargin(String userId);

	// No Tested
	MarketOhlcResponse getMarketOhlc(String instruementToken);
}
