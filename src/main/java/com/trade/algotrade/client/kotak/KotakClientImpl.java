package com.trade.algotrade.client.kotak;

import com.trade.algotrade.client.kotak.dto.CreateOrderDto;
import com.trade.algotrade.client.kotak.dto.KotakOrderDto;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;
import com.trade.algotrade.client.kotak.request.*;
import com.trade.algotrade.client.kotak.response.*;
import com.trade.algotrade.constants.*;
import com.trade.algotrade.dto.IntegrationErrorDetails;
import com.trade.algotrade.dto.Message;
import com.trade.algotrade.enitiy.UserEntity;
import com.trade.algotrade.enums.BrokerEnum;
import com.trade.algotrade.enums.TransactionType;
import com.trade.algotrade.exceptions.BusinessError;
import com.trade.algotrade.exceptions.KotakTradeApiException;
import com.trade.algotrade.exceptions.KotakValidationException;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.service.mapper.UserMapper;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.JsonUtils;
import com.trade.algotrade.utils.SymmetricEncryption;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service("kotakClient")
public class KotakClientImpl implements KotakClient {

    private static final Logger logger = LoggerFactory.getLogger(KotakClientImpl.class);

    @Value("${application.kotakclient.baseurl}")
    private String kotakClientBaseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${application.kotakclient.webSocketUrl}")
    private String webSocketUrl;

    @Autowired
    JsonUtils jsonUtils;

    @Autowired
    private SymmetricEncryption symmetricEncryption;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    CommonUtils commonUtils;

    private String getUrl() {
        return kotakClientBaseUrl;

    }

    private String getWebsocketUrl() {
        return webSocketUrl;
    }

    private UserResponse userResponse;

    private OneTimeTokenResponse getOneTimeToken(UserResponse userDetails) throws Exception {
        logger.debug("******* [KotakClientImpl][OnetimeToken] called for USER ID  := {}", userDetails.getUserId());
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        OnetimeTokenRequest requestData = new OnetimeTokenRequest(userDetails.getUserId(),
                symmetricEncryption.decrypt(userDetails.getPassword()));
        HttpEntity<OnetimeTokenRequest> request = new HttpEntity<>(requestData, headers);
        String url = getUrl() + "/session/1.0/session/login/userid";
        OneTimeTokenResponse body = null;
        BusinessError ottError = null;
        for (int ottRetryAttempt = 0; ottRetryAttempt < Constants.MAX_RETRY_OTT_TOKEN; ottRetryAttempt++) {
            // Resetting this here as in Retry Attempt it should be empty else it has values and its failing to extract fault
            ottError = null;
            try {
                body = restTemplate.exchange(url, HttpMethod.POST, request, OneTimeTokenResponse.class).getBody();
            } catch (HttpStatusCodeException e) {
                logger.error("[** KOTAK OTT TOKEN ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userDetails.getUserId(), e.getMessage());
                String message = "GET ONE TIME TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK OTT TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, Constants.MAX_RETRY_SESSION_TOKEN, message, errorMessage);
                ottError = ErrorCodeConstants.KOTAK_OTT_TOKEN_FAILED;
                waitForNextRetryAttempt(ottRetryAttempt, Constants.MAX_RETRY_SESSION_TOKEN, message, errorMessage);
                continue;
            } catch (Exception e) {
                logger.error("[** KOTAK OTT TOKEN ERROR **] for USER ID := {}> Exception : {}", userDetails.getUserId(), e.getMessage());
                String message = "GET ONE TIME TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK OTT TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, Constants.MAX_RETRY_SESSION_TOKEN, message, errorMessage);
                ottError = ErrorCodeConstants.KOTAK_OTT_TOKEN_FAILED;
                continue;
            }
            if (body != null && body.getFault() != null) {
                logger.info("[** KOTAK OTT TOKEN ERROR **] for USER ID := {} > Fault : {}", userDetails.getUserId(), body.getFault().getMessage());
                ottError = ErrorCodeConstants.KOTAK_OTT_TOKEN_FAILED;
                ottError.setMessage(body.getFault().getMessage());
            }
            break;
        }
        if (Objects.nonNull(ottError)) {
            throw new KotakValidationException(ottError);
        }
        logger.info("******* [KotakClientImpl][OnetimeToken] Completed for USER ID  := {}", userDetails.getUserId());
        return body;
    }

    private void extractKotakExceptionError(Exception e, BusinessError error) {
        int indexOf = e.getMessage().indexOf("{");
        String message = e.getMessage().substring(indexOf, e.getMessage().length()).trim();
        IntegrationErrorDetails fromJson = jsonUtils.fromJson(message, IntegrationErrorDetails.class);
        error.setMessage(fromJson.getFault().getMessage());
        throw new KotakValidationException(error);
    }

    private void extractingKotakExceptionMessage(Exception e, BusinessError error) {
        if (Objects.nonNull(e)) {
            String exceptionMessage = e.getMessage();
            if (StringUtils.isNotBlank(exceptionMessage) && exceptionMessage.contains("{")) {
                int indexOf = exceptionMessage.indexOf("{");
                String message = exceptionMessage.substring(indexOf, e.getMessage().length()).trim();
                IntegrationErrorDetails fromJson = jsonUtils.fromJson(message, IntegrationErrorDetails.class);
                error.setMessage(fromJson.getFault().getMessage());
                throw new KotakTradeApiException(error);
            }
        }
        throw new KotakTradeApiException(error);
    }

    private void setCommonHeaders(final HttpHeaders headers, String kotakConsumerKey) {
        // headers.set("ip", CommonUtils.getHostIp());
        headers.set("ip", "127.0.0.0");
        headers.set("appId", "Test");
        headers.set("consumerKey", kotakConsumerKey);
    }

    private TwoFactorAuthResponse getFreshTwoFactorAuth(String oneTimeToken, UserResponse userDetails) {
        logger.debug("******* [KotakClientImpl][2FactorAuth] called");
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("oneTimeToken", oneTimeToken);
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        TwoFactorAuthRequest requestData = TwoFactorAuthRequest.builder().userid(userDetails.getUserId()).build();
        HttpEntity<TwoFactorAuthRequest> request = new HttpEntity<>(requestData, headers);
        String url = getUrl() + "/session/1.0/session/2FA/oneTimeToken";
        TwoFactorAuthResponse body = null;
        BusinessError twoFactorAuthenticationError = null;
        for (int sessionTokenRetryAttempt = 0; sessionTokenRetryAttempt < Constants.MAX_RETRY_SESSION_TOKEN; sessionTokenRetryAttempt++) {
            twoFactorAuthenticationError = null;
            try {
                body = restTemplate.exchange(url, HttpMethod.POST, request, TwoFactorAuthResponse.class).getBody();
            } catch (HttpStatusCodeException e) {
                logger.error("KOTAK : GET SESSION TOKEN ERROR > HttpStatusCodeException > for USER ID := {} > HttpStatusCodeException : {}", userDetails.getUserId(), e.getMessage());
                if (e.getStatusCode() == HttpStatusCode.valueOf(424)) {
                    logger.info("For Unauthorized scenario fetching session token again for user {} before retry", userDetails.getUserId());
                    oneTimeToken = getOttToken(userDetails.getUserId(), true);
                    headers.set("oneTimeToken", oneTimeToken);
                    request = new HttpEntity<>(requestData, headers);
                }
                String message = "KOTAK : GET SESSION TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "KOTAK : GET SESSION TOKEN > InterruptedException : {}";
                waitForNextRetryAttempt(sessionTokenRetryAttempt, Constants.MAX_RETRY_SESSION_TOKEN, message, errorMessage);
                twoFactorAuthenticationError = ErrorCodeConstants.KOTAK_SESSION_TOKEN_FAILED;
                continue;
            } catch (Exception e) {
                logger.error("[KOTAK : GET SESSION TOKEN > Exception > for USER ID := {} > Exception : {}", userDetails.getUserId(), e.getMessage());
                String message = "KOTAK : GET SESSION TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "KOTAK : GET SESSION TOKEN > InterruptedException : {}";
                waitForNextRetryAttempt(sessionTokenRetryAttempt, Constants.MAX_RETRY_SESSION_TOKEN, message, errorMessage);
                twoFactorAuthenticationError = ErrorCodeConstants.KOTAK_SESSION_TOKEN_FAILED;
                continue;
            }
            if (body != null && body.getFault() != null) {
                logger.info("KOTAK : GET SESSION TOKEN > FAULT > for USER ID := {} > Fault : {}", userDetails.getUserId(), body.getFault().getMessage());
                twoFactorAuthenticationError = ErrorCodeConstants.KOTAK_SESSION_TOKEN_FAILED;
                twoFactorAuthenticationError.setMessage(body.getFault().getMessage());
            }
            break;
        }
        if (Objects.nonNull(twoFactorAuthenticationError)) {
            throw new KotakValidationException(twoFactorAuthenticationError);
        }
        logger.info("******* [KotakClientImpl][2FactorAuth] Completed");
        return body;
    }

    @Override
    public ScripResponse getScripDetails() {

        logger.info("******* [KotakClientImpl][getScripDetails] called");
        UserResponse userDetails = Objects.nonNull(userResponse) ? userResponse : getUserDetails(null);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + "/scripmaster/1.1/filename";
        ScripResponse body = null;
        BusinessError getScriptDetailsError = null;
        try {
            body = restTemplate.exchange(url, HttpMethod.GET, request, ScripResponse.class).getBody();
        } catch (HttpStatusCodeException e) {
            logger.error("[** KOTAK CREATE ORDER ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userDetails.getUserId(), e.getMessage());
            getScriptDetailsError = ErrorCodeConstants.KOTAK_CREATE_ORDER_FAILED;
            getScriptDetailsError.setMessage(e.getMessage());
            throw new KotakValidationException(getScriptDetailsError);
        } catch (Exception e) {
            logger.error("[** KOTAK INSTRUMENT DETAILS ERROR **] > Exception {}", e.getMessage());
            BusinessError businessError = ErrorCodeConstants.KOTAK_INSTRUMENT_DETAILS_FAILED;
            businessError.setMessage(e.getMessage());
            throw new KotakValidationException(businessError);
        }
        if (body != null && body.getFault() != null) {
            logger.info("[** KOTAK MODIFY ORDER ERROR  **] > Fault : {}", body.getFault().getMessage());
            BusinessError scripDetailError = ErrorCodeConstants.KOTAK_INSTRUMENT_DETAILS_FAILED;
            scripDetailError.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(scripDetailError);
        }
        logger.info("******* [KotakClientImpl][getScripDetails] Completed");
        return body;
    }

    @Override
    public WebsocketAccessTokenResponse generateWebSocketAccessToken() {
        logger.info("******* [KotakClientImpl][generateWebSocketAccessToken] called");
        UserResponse userDetails = Objects.nonNull(userResponse) ? userResponse : getUserDetails(null);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        String authentication = Base64.getEncoder().encodeToString(
                new String(userDetails.getConsumerKey() + ":" + userDetails.getConsumerSecrete()).getBytes());
        WebsocketTokenRequest requestData = WebsocketTokenRequest.builder().authentication(authentication).build();
        HttpEntity<WebsocketTokenRequest> request = new HttpEntity<>(requestData, headers);
        String url = getWebsocketUrl() + "/feed/auth/token";
        WebsocketAccessTokenResponse body = null;
        BusinessError createOrderFailed = null;
        for (int wsRetryAttempt = 0; wsRetryAttempt < WebSocketConstant.MAX_RETRY_WEBSOCKET_ACCESS_TOKEN; wsRetryAttempt++) {
            try {
                body = restTemplate.exchange(url, HttpMethod.POST, request, WebsocketAccessTokenResponse.class)
                        .getBody();
                updateWebsocketTone(userDetails, body.getResult().getToken());
                logger.info("******* [KotakClientImpl][generateWebSocketAccessToken] Completed");
            } catch (Exception e) {
                logger.error("[** KOTAK WEBSOCKET ACCESS TOKEN ERROR **] Exception : {}", e.getMessage());
                createOrderFailed = ErrorCodeConstants.KOTAK_WEBSOCKET_ACCESS_TOKEN_FAILED;
                createOrderFailed.setMessage(e.getMessage());
                String message = "GENERATE WEBSOCKET ACCESS TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK WEBSOCKET ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(wsRetryAttempt, WebSocketConstant.MAX_RETRY_LTP, message, errorMessage);
            }

            if (body != null && body.getFault() != null) {
                logger.info("[** KOTAK WEBSOCKET ACCESS TOKEN ERROR  **] > Fault : {}", body.getFault().getMessage());
                createOrderFailed = ErrorCodeConstants.KOTAK_WEBSOCKET_ACCESS_TOKEN_FAILED;
                createOrderFailed.setMessage(body.getFault().getMessage());
                String message = "GENERATE WEBSOCKET ACCESS TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK WEBSOCKET ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(wsRetryAttempt, WebSocketConstant.MAX_RETRY_LTP, message, errorMessage);
            } else {
                break;
            }
        }
        if (Objects.nonNull(createOrderFailed)) {
            throw new KotakValidationException(createOrderFailed);
        }
        return body;
    }


    @Override
    public List<WebsocketAccessTokenResponse> getAllActiveUserOrderWebsocketToken() {
        return userService.getAllActiveUsersByBroker(BrokerEnum.KOTAK_SECURITIES.toString()).stream().map(user -> {
            return getWebsocketFeedToken(user);
        }).collect(Collectors.toList());
    }

    private WebsocketAccessTokenResponse getWebsocketFeedToken(UserResponse user) {
        logger.info("******* [KotakClientImpl][generateWebSocketAccessToken] called");
        UserResponse userDetails = getUserDetails(user.getUserId());
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        String authentication = Base64.getEncoder().encodeToString(
                new String(userDetails.getConsumerKey() + ":" + userDetails.getConsumerSecrete()).getBytes());
        WebsocketTokenRequest requestData = WebsocketTokenRequest.builder().authentication(authentication).build();
        HttpEntity<WebsocketTokenRequest> request = new HttpEntity<>(requestData, headers);
        String url = getWebsocketUrl() + "/feed/auth/token";
        WebsocketAccessTokenResponse body = null;
        BusinessError createOrderFailed = null;
        for (int wsRetryAttempt = 0; wsRetryAttempt < WebSocketConstant.MAX_RETRY_WEBSOCKET_ACCESS_TOKEN; wsRetryAttempt++) {
            try {
                body = restTemplate.exchange(url, HttpMethod.POST, request, WebsocketAccessTokenResponse.class)
                        .getBody();
                updateWebsocketTone(userDetails, body.getResult().getToken());
                logger.info("******* [KotakClientImpl][generateWebSocketAccessToken] Completed");
            } catch (Exception e) {
                logger.error("[** KOTAK WEBSOCKET ACCESS TOKEN ERROR **] Exception : {}", e.getMessage());
                createOrderFailed = ErrorCodeConstants.KOTAK_WEBSOCKET_ACCESS_TOKEN_FAILED;
                createOrderFailed.setMessage(e.getMessage());
                String message = "GENERATE WEBSOCKET ACCESS TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK WEBSOCKET ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(wsRetryAttempt, WebSocketConstant.MAX_RETRY_LTP, message, errorMessage);
            }

            if (body != null && body.getFault() != null) {
                logger.info("[** KOTAK WEBSOCKET ACCESS TOKEN ERROR  **] > Fault : {}", body.getFault().getMessage());
                createOrderFailed = ErrorCodeConstants.KOTAK_WEBSOCKET_ACCESS_TOKEN_FAILED;
                createOrderFailed.setMessage(body.getFault().getMessage());
                String message = "GENERATE WEBSOCKET ACCESS TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK WEBSOCKET ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(wsRetryAttempt, WebSocketConstant.MAX_RETRY_LTP, message, errorMessage);
            } else {
                break;
            }
        }
        if (Objects.nonNull(createOrderFailed)) {
            throw new KotakValidationException(createOrderFailed);
        }
        body.setUserId(user.getUserId());
        return body;
    }

    private void updateWebsocketTone(UserResponse userDetails, String websocketAccessToken) {
        UserEntity userEntity = userService.getUserByUserId(userDetails.getUserId());
        userEntity.setWebsocketToken(websocketAccessToken);
        userService.updateUserEntity(userEntity);
    }

    // No Tested
    @Override
    public MarketDepthResponse getMarektDepth(long instruementToken) {
        logger.info("******* [KotakClientImpl][getMarektDepth] called");
        final HttpHeaders headers = new HttpHeaders();
        UserResponse userDetails = Objects.nonNull(userResponse) ? userResponse : getUserDetails(null);
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + "/quotes/v1.0/depth/instruments/" + instruementToken;
        MarketDepthResponse body;
        try {
            body = restTemplate.exchange(url, HttpMethod.GET, request, MarketDepthResponse.class).getBody();
        } catch (Exception e) {
            logger.error("[** KOTAK MARKET DEPTH ERROR **] > Exception : {}", e.getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_MARKET_DEPTH_FAILED;
            createOrderFailed.setMessage(e.getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        if (body != null && body.getFault() != null) {
            logger.info("[** KOTAK MODIFY ORDER ERROR  **] > Fault : {}", body.getFault().getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_MARKET_DEPTH_FAILED;
            createOrderFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        logger.info("******* [KotakClientImpl][getMarektDepth] Completed");
        return body;
    }

    @Override
    public KotakOrderResponse placeOrder(KotakOrderRequest order, UserResponse userDetails) {
        logger.debug("USER ID := {} : PLACING ORDER AT KOTAK", userDetails.getUserId());
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        String sessionToken = getSessionToken(false).get(userDetails.getUserId());
        headers.set("sessionToken", sessionToken);
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        HttpEntity<KotakOrderRequest> request = new HttpEntity<>(order, headers);
        KotakOrderResponse body = null;
        BusinessError createOrderFailed = null;
        String url = getUrl() + "/orders/1.0/orders";
        String maxValueString = commonUtils.getConfigValue(ConfigConstants.MAX_RETRY_PLACE_ORDER);
        Integer integer = Objects.nonNull(maxValueString) && order.getTransactionType().equals(TransactionType.BUY) ? Integer.valueOf(maxValueString) : Constants.MAX_RETRY_PLACE_ORDER;
        for (int orderRetryAttempt = 0; orderRetryAttempt < integer; orderRetryAttempt++) {
            createOrderFailed = null;
            try {
                if (userDetails.getIsRealTradingEnabled() && !commonUtils.getOffMarketHoursTestFlag()) {
                    body = restTemplate.exchange(url, HttpMethod.POST, request, KotakOrderResponse.class).getBody();
                } else {
                    body = mockRestCall(order);
//                    websocketUtils.subscribeKotakOrderWebsocketMock(body.getSuccess().get("NSE").getOrderId(), userDetails.getUserId(), "150");
//                    Thread.sleep(1000);
                }
            } catch (HttpStatusCodeException e) {
                logger.error("[** KOTAK CREATE ORDER ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userDetails.getUserId(), e.getMessage());
                createOrderFailed = ErrorCodeConstants.KOTAK_CREATE_ORDER_FAILED;
                String message = "KOTAK CREATE ORDER FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK CREATE ORDER ERROR **] InterruptedException : {}";
                //for Unauthorized scenario fetching session token again before retry.
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    logger.info("For Unauthorized scenario fetching session token again for user {} before retry", userDetails.getUserId());
                    sessionToken = getSessionToken(true).get(userDetails.getUserId());
                    headers.set("sessionToken", sessionToken);
                    request = new HttpEntity<>(order, headers);
                }
                waitForNextRetryAttempt(orderRetryAttempt, integer, message, errorMessage);
                continue;
            } catch (Exception e) {
                logger.error("[** KOTAK CREATE ORDER ERROR **] for USER ID := {}> Exception : {}", userDetails.getUserId(), e.getMessage());
                createOrderFailed = ErrorCodeConstants.KOTAK_CREATE_ORDER_FAILED;
                String message = "KOTAK CREATE ORDER FAILED for USER ID ={" + userDetails.getUserId() + "},RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK CREATE ORDER ERROR **] Exception : {" + e.getMessage() + "}";
                waitForNextRetryAttempt(orderRetryAttempt, integer, message, errorMessage);
                continue;
            }
            if (body != null && body.getFault() != null) {
                logger.info("[** KOTAK CREATE ORDER ERROR **] > Fault  Received From Kotak Securities For User ID {} : {}", userDetails.getUserId(), body.getFault().getMessage());
                createOrderFailed = ErrorCodeConstants.KOTAK_CREATE_ORDER_FAILED;
                String message = "KOTAK CREATE ORDER FAILED for USER ID ={" + userDetails.getUserId() + "},RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK CREATE ORDER ERROR **] Business Exception  : {" + body.getFault().getMessage() + "}";
                waitForNextRetryAttempt(orderRetryAttempt, integer, message, errorMessage);
                createOrderFailed.setMessage(body.getFault().getMessage());
            }
            break;
        }
        if (Objects.nonNull(createOrderFailed)) {
            throw new KotakValidationException(createOrderFailed);
        }
        logger.info("USER ID := {} : ORDER PLACED AT KOTAK", userDetails.getUserId());
        return body;
    }

    @Override
    public KotakOrderResponse modifyOrder(KotakOrderRequest kotakOrderRequest, String userId) {
        logger.debug("******* [KotakClientImpl][modifyOrder] called");
        final HttpHeaders headers = new HttpHeaders();
        UserResponse userDetails = getUserDetails(userId);
        setCommonHeaders(headers, userDetails.getConsumerKey());
        String sessionToken = getSessionToken(false).get(userDetails.getUserId());
        headers.set("sessionToken", sessionToken);
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        KotakModifyOrderRequest orderRequest = new KotakModifyOrderRequest();
        BeanUtils.copyProperties(kotakOrderRequest, orderRequest);
        orderRequest.setOrderId(String.valueOf(kotakOrderRequest.getOrderId()));
        HttpEntity<KotakModifyOrderRequest> request = new HttpEntity<>(orderRequest, headers);
        String url = getUrl() + "/orders/1.0/orders";
        KotakOrderResponse body = null;
        BusinessError modifyOrderError = null;
        String maxValueString = commonUtils.getConfigValue(ConfigConstants.MAX_RETRY_PLACE_ORDER);
        Integer integer = Objects.nonNull(maxValueString) && kotakOrderRequest.getTransactionType().equals(TransactionType.BUY)? Integer.valueOf(maxValueString) : Constants.MAX_RETRY_PLACE_ORDER;
        for (int orderRetryAttempt = 0; orderRetryAttempt < integer; orderRetryAttempt++) {
            modifyOrderError = null;
            try {
                if (userDetails.getIsRealTradingEnabled()) {
                    body = restTemplate.exchange(url, HttpMethod.PUT, request, KotakOrderResponse.class).getBody();
                } else {
                    body = mockRestCall(kotakOrderRequest);
                }
            } catch (HttpStatusCodeException e) {
                logger.error("[** KOTAK MODIFY ORDER ERROR **] for order ID := {} > HttpStatusCodeException : {}", kotakOrderRequest.getOrderId(), e.getMessage());
                modifyOrderError = ErrorCodeConstants.KOTAK_MDODIFY_ORDER_FAILED;
                String message = "KOTAK MODIFY ORDER FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK MODIFY ORDER ERROR **] InterruptedException : {}";
                //for Unauthorized scenario fetching session token again before retry.
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    sessionToken = getSessionToken(true).get(userDetails.getUserId());
                    headers.set("sessionToken", sessionToken);
                    request = new HttpEntity<>(orderRequest, headers);
                }
                waitForNextRetryAttempt(orderRetryAttempt, integer, message, errorMessage);
                continue;
            } catch (Exception e) {
                logger.error("[** KOTAK MODIFY ORDER ERROR **] for order Id : {} > Exception : {}", kotakOrderRequest.getOrderId(), e.getMessage());
                modifyOrderError = ErrorCodeConstants.KOTAK_MDODIFY_ORDER_FAILED;
                String message = "KOTAK MODIFY ORDER FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK MODIFY ORDER ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(orderRetryAttempt, integer, message, errorMessage);
                modifyOrderError.setMessage(e.getMessage());
                continue;
            }

            if (body != null && body.getFault() != null) {
                logger.info("[** KOTAK MODIFY ORDER ERROR  **] for order Id : {} > Fault : {}", kotakOrderRequest.getOrderId(), body.getFault().getMessage());
                if (Constants.MODIFY_ORDER_ERROR_IF_ALREADY_FIL.equals(body.getFault().getMessage().trim())) {
                    logger.info("******* [KotakClientImpl][modifyOrder] {} order is already executed at Kotak, hence returning.", kotakOrderRequest.getOrderId());
                    break;
                }
                modifyOrderError = ErrorCodeConstants.KOTAK_MDODIFY_ORDER_FAILED;
                String message = "KOTAK MODIFY ORDER FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK MODIFY ORDER ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(orderRetryAttempt, integer, message, errorMessage);
                modifyOrderError.setMessage(body.getFault().getMessage());
            }
            break;
        }
        if (Objects.nonNull(modifyOrderError)) {
            throw new KotakValidationException(modifyOrderError);
        }
        logger.debug("******* [KotakClientImpl][modifyOrder] Completed");
        return body;
    }

    @Override
    public KotakOrderResponse cancelOrder(Long orderId, String userId) {

        logger.debug("CANCEL KOTAK ORDER : USER ID {}, ORDER ID {} Called", userId, orderId);
        final HttpHeaders headers = new HttpHeaders();
        UserResponse userDetails = getUserDetails(userId);
        setCommonHeaders(headers, userDetails.getConsumerKey());
        String sessionToken = getSessionToken(false).get(userDetails.getUserId());
        headers.set("sessionToken", sessionToken);
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        HttpEntity request = new HttpEntity<>(headers);
        String url = getUrl() + "/orders/1.0/orders/" + orderId;
        KotakOrderResponse body = null;
        String errorMessage = "CANCEL KOTAK ORDER ERROR > USER ID {0} ORDER ID {1}  > InterruptedException : {2}";
        BusinessError cancelOrderError = null;
        String maxValueString = commonUtils.getConfigValue(ConfigConstants.MAX_RETRY_PLACE_ORDER);
        Integer integer = Objects.nonNull(maxValueString) ? Integer.valueOf(maxValueString) : Constants.MAX_RETRY_PLACE_ORDER;
        for (int orderRetryAttempt = 0; orderRetryAttempt < integer; orderRetryAttempt++) {
            cancelOrderError = null;
            try {
                if (userDetails.getIsRealTradingEnabled()) {
                    body = restTemplate.exchange(url, HttpMethod.DELETE, request, KotakOrderResponse.class).getBody();
                }
            } catch (HttpStatusCodeException e) {
                logger.error("CANCEL KOTAK ORDER ERROR > USER ID {} ORDER ID {}  > HttpStatusCodeException : {}", userId, orderId, e.getMessage());
                String message = "CANCEL KOTAK ORDER ERROR > USER ID {0} ORDER ID {1}  > HttpStatusCodeException : {2}";
                message = MessageFormat.format(message, userId, orderId, e.getMessage());
                errorMessage = MessageFormat.format(message, userId, orderId, e.getMessage());
                //for Unauthorized scenario fetching session token again before retry.
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    sessionToken = getSessionToken(true).get(userDetails.getUserId());
                    headers.set("sessionToken", sessionToken);
                    request = new HttpEntity<>(headers);
                }
                waitForNextRetryAttempt(orderRetryAttempt, integer, message, errorMessage);
                cancelOrderError = ErrorCodeConstants.KOTAK_CANCLE_ORDER_FAILED;
                continue;
            } catch (Exception e) {
                logger.error("CANCEL KOTAK ORDER ERROR > USER ID {} ORDER ID {}  > Exception : {}", userId, orderId, e.getMessage());
                cancelOrderError = ErrorCodeConstants.KOTAK_CANCLE_ORDER_FAILED;
                if (body != null && body.getFault() != null) {
                    cancelOrderError.setMessage(body.getFault().getMessage());
                }
                String message = "CANCEL KOTAK ORDER ERROR > USER ID {0} ORDER ID {1}  > Exception : {2}";
                message = MessageFormat.format(message, userId, orderId, e.getMessage());
                errorMessage = MessageFormat.format(errorMessage, userId, orderId, e.getMessage());
                waitForNextRetryAttempt(orderRetryAttempt, integer, message, errorMessage);
                continue;
            }
            if (body != null && body.getFault() != null) {
                logger.info("CANCEL KOTAK ORDER ERROR > USER ID {} ORDER ID {}  > Fault : {}", body.getFault().getMessage());
                cancelOrderError = ErrorCodeConstants.KOTAK_CANCLE_ORDER_FAILED;
                cancelOrderError.setMessage(body.getFault().getMessage());
                String message = "KOTAK CANCEL ORDER FAILED > USER ID {0} ORDER ID {1} FAULT {2} ,RETRYING RETRY ATTEMPT := {3} ";
                message = MessageFormat.format(message, userId, orderId, body.getFault().getMessage(), orderRetryAttempt);
                errorMessage = MessageFormat.format(errorMessage, userId, orderId, body.getFault().getMessage());
                waitForNextRetryAttempt(orderRetryAttempt, integer, message, errorMessage);
            }
            break;
        }
        if (Objects.nonNull(cancelOrderError)) {
            throw new KotakValidationException(cancelOrderError);
        }
        logger.debug("CANCEL KOTAK ORDER : USER ID {}, ORDER ID {} Completed", userId, orderId);
        return null;
    }

    @Override
    public TodaysPositionResponse getTodaysOpenPostions(String userId) {
        logger.info("******* [KotakClientImpl][getTodaysOpenPostions] called");
        UserResponse userDetails = getUserDetails(userId);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        String sessionToken = getSessionToken(false).get(userId);
        headers.set("sessionToken", sessionToken);
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + "/positions/1.0/positions/todays";
        TodaysPositionResponse body = null;
        try {
            body = restTemplate.exchange(url, HttpMethod.GET, request, TodaysPositionResponse.class).getBody();
        } catch (HttpStatusCodeException e) {
            logger.error("[** KOTAK GET ALL ORDER ERROR**] > HttpStatusCodeException : {}", e.getMessage());
            //for Unauthorized scenario fetching session token again before retry.
            if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                logger.info("For Unauthorized scenario fetching session token again for user {} before retry", userDetails.getUserId());
                sessionToken = getSessionToken(true).get(userDetails.getUserId());
                headers.set("sessionToken", sessionToken);
                request = new HttpEntity<>(headers);
            }
            BusinessError getAllOrdersFailed = ErrorCodeConstants.KOTAK_TADAYS_POSITION_FAILED;
            extractKotakExceptionError(e, getAllOrdersFailed);
        } catch (Exception e) {
            logger.error("[** KOTAK TODAYS POSITION ERROR **] > Exception : {}", e.getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_TADAYS_POSITION_FAILED;
            createOrderFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        if (body != null && body.getFault() != null) {
            logger.info("[** KOTAK TODAYS POSITION ERROR  **] > Fault : {}", body.getFault().getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_TADAYS_POSITION_FAILED;
            createOrderFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        logger.info("******* [KotakClientImpl][getTodaysOpenPostions] Completed");
        return body;
    }

    @Override
    public OpenPositionsResponse getAllOpenPositions(String userId) {
        logger.info("******* [KotakClientImpl][getAllOpenPositions] called");
        UserResponse userDetails = getUserDetails(userId);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        headers.set("sessionToken", getSessionToken(false).get(userId));
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + "/positions/1.0/positions/open";
        OpenPositionsResponse body = null;
        try {
            if (userDetails.getIsRealTradingEnabled()) {
                body = restTemplate.exchange(url, HttpMethod.GET, request, OpenPositionsResponse.class).getBody();
            } else {
                //This will be mock response from Json file
                File resource = new ClassPathResource(
                        "kotak-mock-response/Response_all_open_positions.json").getFile();
                body = jsonUtils.fromJson(resource, OpenPositionsResponse.class);
            }
        } catch (HttpStatusCodeException e) {
            logger.error("[** KOTAK GET ALL ORDER ERROR**] > HttpStatusCodeException : {}", e.getMessage());
            BusinessError getAllOrdersFailed = ErrorCodeConstants.KOTAK_TADAYS_POSITION_FAILED;
            extractKotakExceptionError(e, getAllOrdersFailed);
        } catch (Exception e) {
            logger.error("[** KOTAK GET ALL OPEN POSITION ERROR **] > Exception : {}", e.getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_ALL_OPEN_POSITION_FAILED;
            createOrderFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        if (body != null && body.getFault() != null) {
            logger.info("[** KOTAK GET ALL OPEN POSITION ERROR  **] > Fault : {}", body.getFault().getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_ALL_OPEN_POSITION_FAILED;
            createOrderFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        logger.info("******* [KotakClientImpl][getAllOpenPositions] Completed");
        return body;
    }

    @Override
    public HoldingPositionResponse getSoldHoldingPositions(String userId) {
        logger.info("******* [KotakClientImpl][getSoldHoldingPositions] called");
        UserResponse userDetails = getUserDetails(userId);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("Authorization", "Bearer " + userDetails);
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + "/positions/1.0/positions/stocks";
        HoldingPositionResponse body = null;
        try {
            body = restTemplate.exchange(url, HttpMethod.GET, request, HoldingPositionResponse.class).getBody();
        } catch (HttpStatusCodeException e) {
            logger.error("[** KOTAK GET ALL ORDER ERROR**] > HttpStatusCodeException : {}", e.getMessage());
            BusinessError getAllOrdersFailed = ErrorCodeConstants.KOTAK_ALL_OPEN_POSITION_FAILED;
            extractKotakExceptionError(e, getAllOrdersFailed);
        } catch (Exception e) {
            logger.error("[** KOTAK GET HOLDING POSITION ERROR **] > Exception : {}", e.getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_ALL_OPEN_POSITION_FAILED;
            createOrderFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        if (body != null && body.getFault() != null) {
            logger.info("[** KOTAK GET HOLDING POSITION ERROR **] > Fault : {}", body.getFault().getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_ALL_OPEN_POSITION_FAILED;
            createOrderFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        logger.info("******* [KotakClientImpl][getSoldHoldingPositions] Completed");
        return body;
    }

    @Override
    public KotakGetOrderResponse getOrderDetailsById(Long orderId, String userId, boolean isFnoOrder) {

        logger.info("******* [KotakClientImpl][getOrderDetailsById] called");
        UserResponse userDetails = getUserDetails(userId);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("sessionToken", getSessionToken(false).get(userId));
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);
        String fno = isFnoOrder ? "Y" : "N";
        String url = getUrl() + "/reports/1.0/orders/" + orderId + "/" + fno;
        KotakGetOrderResponse body = null;
        try {
            body = restTemplate.exchange(url, HttpMethod.GET, request, KotakGetOrderResponse.class).getBody();
        } catch (Exception e) {
            logger.error("[** KOTAK GET HOLDING POSITION ERROR **] > Exception :  {}", e.getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_GET_ORDER_FAILED;
            createOrderFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        if (body != null && body.getFault() != null) {
            logger.info("[** KOTAK GET HOLDING POSITION ERROR **] > Fault : {}", body.getFault().getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_GET_ORDER_FAILED;
            createOrderFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(createOrderFailed);
        }
        logger.info("******* [KotakClientImpl][getOrderDetailsById] Completed");
        return body;
    }

    @Override
    public KotakGetAllOrderResponse getAllOrders(String userId) {
        logger.debug("******* [KotakClientImpl][getAllOrders] called");
        UserResponse userDetails = getUserDetails(userId);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("sessionToken", getSessionToken(false).get(userId));
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + "/reports/1.0/orders";
        KotakGetAllOrderResponse body = null;
        String errorMessage = "GET ALL KOTAK ORDERS ERROR > USER ID {0} ORDER ID {1}  > InterruptedException : {2}";
        BusinessError getAllOrdersError = null;
        String maxValueString = commonUtils.getConfigValue(ConfigConstants.MAX_RETRY_PLACE_ORDER);
        Integer integer = Objects.nonNull(maxValueString) ? Integer.valueOf(maxValueString) : Constants.MAX_RETRY_PLACE_ORDER;
        for (int orderRetryAttempt = 0; orderRetryAttempt < integer; orderRetryAttempt++) {
            getAllOrdersError = null;
            try {
                if (userDetails.getIsRealTradingEnabled() && !CommonUtils.getOffMarketHoursTestFlag()) {
                    body = restTemplate.exchange(url, HttpMethod.GET, request, KotakGetAllOrderResponse.class).getBody();
                } else {
                    body = mockGetAllOrders();
                }
            } catch (HttpStatusCodeException e) {
                logger.error("[** KOTAK GET ALL ORDER ERROR**] > HttpStatusCodeException : {}", e.getMessage());
                getAllOrdersError = ErrorCodeConstants.KOTAK_GET_ALL_ORDER_FAILED;
                waitForNextRetryAttempt(orderRetryAttempt, integer, getAllOrdersError.getMessage(), errorMessage);
                continue;
            } catch (Exception e) {
                logger.error("[** KOTAK GET ALL ORDER ERROR **] > Exception : {}", e.getMessage());
                getAllOrdersError = ErrorCodeConstants.KOTAK_GET_ALL_ORDER_FAILED;
                getAllOrdersError.setMessage(e.getMessage());
                waitForNextRetryAttempt(orderRetryAttempt, integer, getAllOrdersError.getMessage(), errorMessage);
                continue;
            }
            if (body != null && body.getFault() != null) {
                logger.info("[** KOTAK GET ALL ORDER ERROR **] > Fault : {}", body.getFault().getMessage());
                getAllOrdersError = ErrorCodeConstants.KOTAK_GET_ALL_ORDER_FAILED;
                getAllOrdersError.setMessage(body.getFault().getMessage());
                waitForNextRetryAttempt(orderRetryAttempt, integer, getAllOrdersError.getMessage(), errorMessage);
            }
            break;
        }
        if (Objects.nonNull(getAllOrdersError)) {
            throw new KotakValidationException(getAllOrdersError);
        }
        logger.debug("******* [KotakClientImpl][getAllOrders] Completed");
        return body;
    }

    @Override
    public Map<String, String> getSessionToken(boolean isExpired) {
        Map<String, String> sessionToken = new HashMap<>();
        userService.getAllActiveUsersByBroker(BrokerEnum.KOTAK_SECURITIES.toString()).stream()
                .forEach(userDetails -> {
                    Map<String, String> tokenMap = new HashMap<>();
                    String ottToken;
                    if ((CommonUtils.getTokenMap().get(userDetails.getUserId()) == null
                            || StringUtils.isBlank(CommonUtils.getTokenMap().get(userDetails.getUserId()).get("OTT"))) || isExpired) {
                        ottToken = getFreshOneTimeToken(userDetails, tokenMap);
                    } else {
                        ottToken = CommonUtils.getTokenMap().get(userDetails.getUserId()).get("OTT");
                    }
                    if ((CommonUtils.getTokenMap().get(userDetails.getUserId()) == null
                            || StringUtils.isBlank(CommonUtils.getTokenMap().get(userDetails.getUserId()).get("2FA"))) || isExpired) {
                        getFreshTwoFactorSessionToken(sessionToken, userDetails, tokenMap, ottToken);
                    } else {
                        sessionToken.put(userDetails.getUserId(),
                                CommonUtils.getTokenMap().get(userDetails.getUserId()).get("2FA"));
                    }
                });
        return sessionToken;
    }

    public String getOttToken(String userId, boolean isExpired) {
        UserResponse userDetails = userService.getUserById(userId);
        Map<String, String> tokenMap = new HashMap<>();
        String ottToken;
        if ((CommonUtils.getTokenMap().get(userDetails.getUserId()) == null
                || StringUtils.isBlank(CommonUtils.getTokenMap().get(userDetails.getUserId()).get("OTT"))) || isExpired) {
            ottToken = getFreshOneTimeToken(userDetails, tokenMap);
        } else {
            ottToken = CommonUtils.getTokenMap().get(userDetails.getUserId()).get("OTT");
        }
        return ottToken;
    }

    private void getFreshTwoFactorSessionToken(Map<String, String> sessionToken, UserResponse userDetails, Map<String, String> tokenMap, String ottToken) {
        TwoFactorAuthResponse twoFactorAuth = getFreshTwoFactorAuth(ottToken, userDetails);
        if (twoFactorAuth == null || twoFactorAuth.getSuccess() == null
                || StringUtils.isEmpty(twoFactorAuth.getSuccess().getSessionToken())) {
            BusinessError emptyOrNullTokenError = ErrorCodeConstants.EMPTY_NULL_OR_SESSION_TOKEN_ERROR;
            emptyOrNullTokenError.setStatus(HttpStatus.BAD_REQUEST);
            throw new KotakTradeApiException(emptyOrNullTokenError);
        } else {
            String token = twoFactorAuth.getSuccess().getSessionToken();
            tokenMap.put("2FA", token);
            sessionToken.put(userDetails.getUserId(), token);
            CommonUtils.getTokenMap().put(userDetails.getUserId(), tokenMap);
        }
    }

    private String getFreshOneTimeToken(UserResponse userDetails, Map<String, String> tokenMap) {
        String ottToken;
        OneTimeTokenResponse oneTimeToken;
        try {
            oneTimeToken = getOneTimeToken(userDetails);
        } catch (Exception e) {
            BusinessError emptyOrNullTokenError = ErrorCodeConstants.USER_PASSWORD_DECRYPTION_FAILED;
            emptyOrNullTokenError.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            throw new KotakTradeApiException(emptyOrNullTokenError);
        }
        if (oneTimeToken == null || oneTimeToken.getSuccess() == null
                || StringUtils.isEmpty(oneTimeToken.getSuccess().getOneTimeToken())) {
            BusinessError emptyOrNullTokenError = ErrorCodeConstants.EMPTY_OR_NULL_TOKEN_ERROR;
            emptyOrNullTokenError.setStatus(HttpStatus.BAD_REQUEST);
            throw new KotakTradeApiException(emptyOrNullTokenError);
        } else {
            ottToken = oneTimeToken.getSuccess().getOneTimeToken();
            tokenMap.put("OTT", ottToken);
            CommonUtils.getTokenMap().put(userDetails.getUserId(), tokenMap);
        }
        return ottToken;
    }

    private UserResponse getUserDetails(String userId) {
        if (Objects.isNull(userId)) {
            Map.Entry<String, String> entry = getSessionToken(false).entrySet().iterator().next();
            Optional<UserResponse> userDetails = userService.getUsers().stream()
                    .filter(user -> user.getUserId().equals(entry.getKey())).findAny();
            userDetails.ifPresent(response -> userResponse = response);
        } else {
            Optional<UserResponse> userDetails = userService.getUsers().stream()
                    .filter(user -> user.getUserId().equals(userId)).findAny();
            userDetails.ifPresent(response -> userResponse = response);
        }
        return userResponse;
    }

    @Override
    public LtpResponse getLtp(String instruementToken) {
        logger.debug(" *** [getLtp] Called *** Instrument Token : = {}", instruementToken);
        final HttpHeaders headers = new HttpHeaders();
        UserResponse userDetails = Objects.nonNull(userResponse) ? userResponse : getUserDetails(null);
        setCommonHeaders(headers, userDetails.getConsumerKey());
        String accessToken = userDetails.getAccessToken();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + "/quotes/v1.0/ltp/instruments/" + instruementToken;
        LtpResponse body = null;
        BusinessError getLtpBusinessError = null;
        for (int wsRetryAttempt = 0; wsRetryAttempt < WebSocketConstant.MAX_RETRY_LTP; wsRetryAttempt++) {
            try {
//                Thread.sleep(5000);
                if (userDetails.getIsRealTradingEnabled() && !CommonUtils.getOffMarketHoursTestFlag()) {
                    body = restTemplate.exchange(url, HttpMethod.GET, request, LtpResponse.class).getBody();
                } else {
                    body = mockLtpResponse();
                }
            } catch (HttpStatusCodeException e) {
                logger.error("[** KOTAK CREATE ORDER ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userDetails.getUserId(), e.getMessage());
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    logger.info("For Unauthorized scenario fetching session token again for user {} before retry", userDetails.getUserId());
                    accessToken = getSessionToken(true).get(userDetails.getUserId());
                    headers.set("Authorization", "Bearer " + accessToken);
                    request = new HttpEntity<>(headers);
                }
                String message = "KOTAK GET LTP FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK GET LTP ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(wsRetryAttempt, WebSocketConstant.MAX_RETRY_LTP, message, errorMessage);
                getLtpBusinessError = ErrorCodeConstants.KOTAK_CREATE_ORDER_FAILED;
                continue;
            } catch (Exception e) {
                logger.error("[** KOTAK GET LTP ERROR **] for USER ID := {}> Exception : {}", userDetails.getUserId(), e.getMessage());
                String message = "KOTAK GET LTP FAILED for USER ID ={" + userDetails.getUserId() + "},RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** KOTAK GET LTP ERROR **] Exception : {" + e.getMessage() + "}";
                waitForNextRetryAttempt(wsRetryAttempt, WebSocketConstant.MAX_RETRY_LTP, message, errorMessage);
                getLtpBusinessError = ErrorCodeConstants.KOTAK_CREATE_ORDER_FAILED;
            }
            if (body != null && body.getFault() != null) {
                logger.info("[** KOTAK LTP ERROR **] >  Instrument Token : = {} Fault := {}", instruementToken, body.getFault().getMessage());
                BusinessError ltpFailed = ErrorCodeConstants.KOTAK_LTP_FAILED;
                ltpFailed.setMessage(body.getFault().getMessage());
                String message = "GET LTP FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[**  KOTAK LTP ERROR **] InterruptedException := {}";
                waitForNextRetryAttempt(wsRetryAttempt, WebSocketConstant.MAX_RETRY_LTP, message, errorMessage);
                //throw new KotakValidationException(ltpFailed);
            }
            break;
        }
        return body;
    }

    @Override
    public MarginResponse getMargin(String userId) {

        logger.info("******* [KotakClientImpl][getMargin] called USER ID : = {}", userId);
        UserResponse userDetails = getUserDetails(userId);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("sessionToken", getSessionToken(false).get(userId));
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + "/margin/1.0/margin";
        MarginResponse body = null;
        try {
            body = restTemplate.exchange(url, HttpMethod.GET, request, MarginResponse.class).getBody();
        } catch (HttpStatusCodeException e) {
            logger.error("[** KOTAK GET MARGIN ERROR**]  USER ID : = {} > HttpStatusCodeException : {}", userId, e.getMessage());
            BusinessError marginFailed = ErrorCodeConstants.KOTAK_GET_MARGIN_FAILED;
            extractKotakExceptionError(e, marginFailed);
        } catch (Exception e) {
            logger.error("[** KOTAK GET MARGIN ERROR **]  USER ID : = {} > Exception : {}", userId, e.getMessage());
            BusinessError marginFailed = ErrorCodeConstants.KOTAK_GET_MARGIN_FAILED;
            marginFailed.setMessage(e.getMessage());
            throw new KotakTradeApiException(marginFailed);
        }
        if (body != null && body.getFault() != null) {
            logger.info("[** KOTAK GET MARGIN ERROR **] USER ID := {} > Fault : {}", userId, body.getFault().getMessage());
            BusinessError marginFailed = ErrorCodeConstants.KOTAK_GET_MARGIN_FAILED;
            marginFailed.setMessage(body.getFault().getMessage());
            throw new KotakValidationException(marginFailed);
        }
        logger.info("******* [KotakClientImpl][getMargin] Completed for USER : = {}", userId);
        return body;
    }

    private KotakOrderResponse mockRestCall(KotakOrderRequest order) {
        logger.info("******* [KotakClientImpl] Mocking place order request.");
        KotakOrderResponse body;
        long number = (long) Math.floor(Math.random() * 9000000000000L) + 1000000000000L;
        Map<String, CreateOrderDto> map = new HashMap<>();
        CreateOrderDto createOrderDto = new CreateOrderDto();
        createOrderDto.setMessage("Your mock order has been Placed and Forwarded to the Exchange:" + number);
        createOrderDto.setOrderId(number);
        createOrderDto.setPrice(order.getPrice());
        createOrderDto.setQuantity((int) order.getQuantity());
        createOrderDto.setTag("MOCKED" + order.getTag());
        map.put("NSE", createOrderDto);
        body = new KotakOrderResponse();
        body.setSuccess(map);
        return body;
    }

    private void waitForNextRetryAttempt(Integer currentAttempt, Integer maxAttempt, String message, String errorMessage) {
        if (currentAttempt < maxAttempt) {
            try {
                int timeout = 300;
                if(currentAttempt != 0)
                    timeout = timeout * 2 * currentAttempt;

                Thread.sleep(timeout);// wait half second and retry
            } catch (InterruptedException e) {
                logger.error(errorMessage,
                        e.getMessage());
            }
            logger.info(message, currentAttempt);
        }
    }

    @Override
    public MarketOhlcResponse getMarketOhlc(String instruementToken) {
        logger.debug(" *** [getLtp] Called *** Instrument Token : = {}", instruementToken);
        final HttpHeaders headers = new HttpHeaders();
        UserResponse userDetails = Objects.nonNull(userResponse) ? userResponse : getUserDetails(null);
        setCommonHeaders(headers, userDetails.getConsumerKey());
        headers.set("Authorization", "Bearer " + userDetails.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + "/quotes/v1.0/ohlc/instruments/" + instruementToken;
        MarketOhlcResponse body = null;

        for (int wsRetryAttempt = 0; wsRetryAttempt < WebSocketConstant.MAX_RETRY_LTP; wsRetryAttempt++) {
            try {
                body = restTemplate.exchange(url, HttpMethod.GET, request, MarketOhlcResponse.class).getBody();
            } catch (Exception e) {
                logger.error("[** KOTAK OHLC ERROR **] > Exception : {}", e.getMessage());
                BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_LTP_FAILED;
//                createOrderFailed.setMessage(e.getMessage());
                createOrderFailed.setMessage("** Kotak LTP Error : Body is null. **");
                String message = "GET OHLC FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[**  KOTAK LTP ERROR **] InterruptedException := {}";
                waitForNextRetryAttempt(wsRetryAttempt, WebSocketConstant.MAX_RETRY_LTP, message, errorMessage);
                //throw new KotakValidationException(createOrderFailed);
            }
            if (body != null && body.getFault() != null) {
                logger.info("[** KOTAK OHLC ERROR **] > Fault := {}", body.getFault().getMessage());
                BusinessError ltpFailed = ErrorCodeConstants.KOTAK_LTP_FAILED;
                ltpFailed.setMessage(body.getFault().getMessage());
                String message = "GET OHLC FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[**  KOTAK LTP ERROR **] InterruptedException := {}";
                waitForNextRetryAttempt(wsRetryAttempt, WebSocketConstant.MAX_RETRY_LTP, message, errorMessage);
                //throw new KotakValidationException(ltpFailed);
            }
            break;
        }
        return body;
    }

    private KotakGetAllOrderResponse mockGetAllOrders(){
        KotakGetAllOrderResponse kotakGetAllOrderResponse = new KotakGetAllOrderResponse();
        List<KotakOrderDto> orderDtoList = new ArrayList<>();
        KotakOrderDto kotakOrderDto = new KotakOrderDto();
        kotakOrderDto.setCancelledQuantity(0);
        kotakOrderDto.setDisclosedQuantity(0L);
        kotakOrderDto.setExchangeOrderID("1500000020440802");
        kotakOrderDto.setExpiryDate("29MAR23");
        kotakOrderDto.setFilledQuantity(60);
        kotakOrderDto.setInstrumentName("BANKNIFTY");
        kotakOrderDto.setInstrumentToken(16313L);
        kotakOrderDto.setOptionType("CE");
        kotakOrderDto.setOrderId(2230327033569L);
        kotakOrderDto.setOrderQuantity(60);
        kotakOrderDto.setPendingQuantity(0);
        kotakOrderDto.setPrice(new BigDecimal(150));
        kotakOrderDto.setStatus("TRAD");
        orderDtoList.add(kotakOrderDto);
        kotakGetAllOrderResponse.setSuccess(orderDtoList);
        return kotakGetAllOrderResponse;
    }

    private LtpResponse mockLtpResponse(){
        LtpResponse ltpResponse = new LtpResponse();
        LtpSuccess ltpSuccess = new LtpSuccess();
        ltpSuccess.setLastPrice(new BigDecimal(130));
        ltpResponse.setSuccess(Collections.singletonList(ltpSuccess));
        return ltpResponse;
    }
}