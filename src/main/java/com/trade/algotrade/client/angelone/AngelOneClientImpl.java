package com.trade.algotrade.client.angelone;

import com.trade.algotrade.client.angelone.enums.ExchangeType;
import com.trade.algotrade.client.angelone.enums.QuoteMode;
import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.client.angelone.request.GetAccessTokenRequest;
import com.trade.algotrade.client.angelone.request.GetCandleRequest;
import com.trade.algotrade.client.angelone.request.MarginUserRequest;
import com.trade.algotrade.client.angelone.request.RenewAccessTokenRequest;
import com.trade.algotrade.client.angelone.request.feed.GetLtpRequest;
import com.trade.algotrade.client.angelone.request.feed.QuoteRequest;
import com.trade.algotrade.client.angelone.request.orders.AngelAllOrdersResponse;
import com.trade.algotrade.client.angelone.request.orders.AngelOrderRequest;
import com.trade.algotrade.client.angelone.request.orders.AngelOrderResponse;
import com.trade.algotrade.client.angelone.request.orders.OrderDetailDto;
import com.trade.algotrade.client.angelone.response.feed.QouteResponse;
import com.trade.algotrade.client.angelone.response.historical.GetCandleResponse;
import com.trade.algotrade.client.angelone.response.user.*;
import com.trade.algotrade.constants.*;
import com.trade.algotrade.enums.BrokerEnum;
import com.trade.algotrade.exceptions.AngelOneValidationException;
import com.trade.algotrade.exceptions.BusinessError;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.SymmetricEncryption;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service("angeloneClient")
public class AngelOneClientImpl implements AngelOneClient {

    private static final Logger logger = LoggerFactory.getLogger(AngelOneClientImpl.class);

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private UserService userService;

    @Value("${application.angelOneClient.baseurl}")
    private String kotakClientBaseUrl;


    @Autowired
    private SymmetricEncryption symmetricEncryption;

    @Autowired
    CommonUtils commonUtils;

    private UserResponse userResponse;

    private String getUrl() {
        return kotakClientBaseUrl;

    }

    private void setCommonHeaders(final HttpHeaders headers, UserResponse userResponse) {
        // headers.set("ip", CommonUtils.getHostIp());
        headers.set("X-ClientLocalIP", "62c0:55a7:6219:794%4");
        headers.set("accept", "application/json");
        headers.set("X-ClientPublicIP", "192.168.1.34");
        headers.set("X-PrivateKey", userResponse.getConsumerKey());
        headers.set("X-UserType", "USER");
        headers.set("X-SourceID", "WEB");
        headers.set("X-MACAddress", "94-65-9C-22-79-D2");
        headers.set("Content-Type", "application/json");
    }

    private void waitForNextRetryAttempt(Integer currentAttempt, Integer maxAttempt, String message, String errorMessage) {
        if (currentAttempt < maxAttempt) {
            try {
                Thread.sleep(700);// wait half second and retry
            } catch (InterruptedException e) {
                logger.error(errorMessage,
                        e.getMessage());
            }
            logger.info(message, currentAttempt);
        }
    }


    @Override
    public GetAccessTokenResponse getAccessToken(UserResponse userResponse) {
        logger.info("ACCESS TOKEN : ANGELONE called for USER ID  := {}", userResponse.getUserId());
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userResponse);
        GetAccessTokenRequest getAccessTokenRequest = null;
        try {
            getAccessTokenRequest = GetAccessTokenRequest.builder()
                    .clientcode(userResponse.getUserId())
                    .password(symmetricEncryption.decrypt(userResponse.getPassword()))
                    .totp(TOTPUtility.getTOTPCode(userResponse.getTotpSecrete())).build();
        } catch (Exception e) {
            logger.error("Password Decrypt Error {}", e);
            throw new RuntimeException(e);
        }

        HttpEntity<GetAccessTokenRequest> request = new HttpEntity<>(getAccessTokenRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.LOGIN_BY_USERPASSWROD_URL;
        GetAccessTokenResponse body = null;
        BusinessError getAcessToken = null;
        for (int ottRetryAttempt = 0; ottRetryAttempt < Constants.MAX_RETRY_OTT_TOKEN; ottRetryAttempt++) {
            try {
                body = restTemplate.exchange(url, HttpMethod.POST, request, GetAccessTokenResponse.class).getBody();
            } catch (HttpStatusCodeException e) {
                logger.error("[** ANGELONE RENEW ACCESS TOKEN ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userResponse.getUserId(), e.getMessage());
                String message = "GET ACCESS TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE RENEW ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessToken = AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_FAILED;
                continue;
            } catch (Exception e) {
                logger.error("[** ANGELONE RENEW ACCESS TOKEN ERROR **] for USER ID := {}> Exception : {}", userResponse.getUserId(), e.getMessage());
                String message = "GET ACCESS TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE RENEW ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessToken = AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_FAILED;
                continue;
            }
            if (body != null && !body.getStatus()) {
                logger.info("[** ANGELONE RENEW ACCESS TOKEN ERROR **] for USER ID := {} > Fault : {}", userResponse.getUserId(), body.getMessage());
                getAcessToken = AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_FAILED;
                getAcessToken.setMessage(body.getMessage());
                String message = "GET ACCESS TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE RENEW ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
            }
            break;
        }
        if (Objects.nonNull(body) && body.getStatus()) {
            body.setUserId(userResponse.getUserId());
        }
        logger.info("ACCESS TOKEN : ANGELONE Completed for USER ID  := {}", userResponse.getUserId());
        return body;
    }

    @Override
    public List getScripDetails() {
        logger.info("******* [KotakClientImpl][getScripDetails] called");
        final HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = "https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json";
        List instrumentResponses;
        try {
            instrumentResponses = restTemplate.exchange(url, HttpMethod.GET, request, List.class).getBody();
        } catch (Exception e) {
            logger.error("[** KOTAK INSTRUMENT DETAILS ERROR **] > Exception {}", e.getMessage());
            BusinessError createOrderFailed = ErrorCodeConstants.KOTAK_INSTRUMENT_DETAILS_FAILED;
            createOrderFailed.setMessage(e.getMessage());
            throw new AngelOneValidationException(createOrderFailed);
        }
        logger.info("******* [KotakClientImpl][getScripDetails] Completed");
        return instrumentResponses;
    }

    @Override
    public List<GetAccessTokenResponse> getAccessTokenForAllActiveUsers() {
        List<GetAccessTokenResponse> list = new ArrayList<>();
        userService.getAllActiveUsersByBroker(BrokerEnum.ANGEL_ONE.toString()).forEach(userResponse -> {
            GetAccessTokenResponse accessToken = getUserAccessToken(userResponse);
            if (accessToken != null) {
                list.add(accessToken);
            }
            //RenewAccessTokenResponse renewToken = renewAccessToken(accessToken.getData().getRefreshToken(), userResponse);
        });
        return list;
    }

    public GetAccessTokenResponse getAccessTokenForSystemUsers() {
        List<GetAccessTokenResponse> list = new ArrayList<>();
        UserResponse userResponse = userService.getActiveBrokerSystemUser(BrokerEnum.ANGEL_ONE.toString());
        return getAccessToken(userResponse);
    }

    @Override
    public RenewAccessTokenResponse renewAccessToken(String currentAccessToken, UserResponse userResponse) {
        logger.info("******* [AngelOneClientImpl][ ANGELONE RENEW ACCESS] called for USER ID  := {}", userResponse.getUserId());
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userResponse);
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(currentAccessToken));
        RenewAccessTokenRequest renewAccessTokenRequest = null;
        try {
            renewAccessTokenRequest = RenewAccessTokenRequest.builder().refreshToken(userResponse.getRefreshToken()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpEntity<RenewAccessTokenRequest> request = new HttpEntity<>(renewAccessTokenRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.RENEW_ACCESS_URL;
        RenewAccessTokenResponse body = null;
        BusinessError getAcessToken;
        Exception exception = null;
        for (int ottRetryAttempt = 0; ottRetryAttempt < Constants.MAX_RETRY_OTT_TOKEN; ottRetryAttempt++) {
            try {
                body = restTemplate.exchange(url, HttpMethod.POST, request, RenewAccessTokenResponse.class).getBody();
            } catch (HttpStatusCodeException e) {
                logger.error("[** ANGELONE RENEW ACCESS TOKEN ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE RENEW ACCESS TOKEN ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE RENEW ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessToken = AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_FAILED;
                exception = e;
            } catch (Exception e) {
                logger.error("[** ANGELONE RENEW ACCESS TOKEN ERROR **] for USER ID := {}> Exception : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE RENEW ACCESS TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE RENEW ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessToken = AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_FAILED;
                exception = e;
            }
            if (body != null && !body.getStatus()) {
                logger.info("[** ANGELONE RENEW ACCESS TOKEN ERROR **] for USER ID := {} > Fault : {}", userResponse.getUserId(), body.getMessage());
                getAcessToken = AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_FAILED;
                getAcessToken.setMessage(body.getMessage());
                String message = "ANGELONE RENEW ACCESS TOKEN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE RENEW ACCESS TOKEN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
            }
            if (Objects.nonNull(body) && body.getStatus()) {
                break;
            }
        }
        body.setUserId(userResponse.getUserId());
        logger.info("******* [AngelOneClientImpl][ANGELONE RENEW ACCESS] Completed for USER ID  := {}", userResponse.getUserId());
        return body;
    }

    @Override
    public GetMarginResponse getMargin(String currentAccessToken, UserResponse userResponse) {
        logger.info("******* [AngelOneClientImpl][GET USER MARGIN] called for USER ID  := {}", userResponse.getUserId());
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userResponse);
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(currentAccessToken));
        MarginUserRequest userMarginRequest = MarginUserRequest.builder().clientcode(userResponse.getUserId()).build();
        HttpEntity<MarginUserRequest> request = new HttpEntity<>(userMarginRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.GET_USER_MARGIN;
        GetMarginResponse body = null;
        BusinessError getAcessTokenError = null;
        for (int ottRetryAttempt = 0; ottRetryAttempt < Constants.MAX_RETRY_OTT_TOKEN; ottRetryAttempt++) {
            getAcessTokenError = null;
            try {
                body = restTemplate.exchange(url, HttpMethod.GET, request, GetMarginResponse.class).getBody();
            } catch (HttpStatusCodeException e) {
                logger.error("[** ANGELONE GET USER MARGIN ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE GET USER MARGIN ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE GET USER MARGIN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessTokenError = AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_FAILED;
                continue;
            } catch (Exception e) {
                logger.error("[** ANGELONE GET USER MARGIN ERROR **] for USER ID := {}> Exception : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE GET USER MARGIN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE GET USER MARGIN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessTokenError = AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_FAILED;
            }
            if (body != null && !body.getStatus()) {
                logger.info("[** ANGELONE GET USER MARGIN ERROR **] for USER ID := {} > Fault : {}", userResponse.getUserId(), body.getMessage());
                getAcessTokenError = AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_FAILED;
                getAcessTokenError.setMessage(body.getMessage());
                String message = "ANGELONE GET USER MARGIN FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE GET USER MARGIN ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
            }
            break;
        }
        if (Objects.nonNull(getAcessTokenError)) {
            throw new AngelOneValidationException(getAcessTokenError);
        }
        logger.info("******* [AngelOneClientImpl][GET USER MARGIN] Completed for USER ID  := {}", userResponse.getUserId());
        return body;
    }

    @Override
    public GetUserProfileResponse getUserProfile() {
        return null;
    }

    @Override
    public LogoutUserResponse logoutUser(String currentAccessToken, UserResponse userResponse) {
        logger.info("******* [AngelOneClientImpl][LOGOUT USER] called for USER ID  := {}", userResponse.getUserId());
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userResponse);
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(currentAccessToken));
        HttpEntity<String> request = new HttpEntity<>(headers);
        String url = getUrl() + AngelOneRouteConstants.LOGOUT_USER;
        LogoutUserResponse body = null;
        BusinessError getAcessToken = null;
        Exception exception = null;
        for (int ottRetryAttempt = 0; ottRetryAttempt < Constants.MAX_RETRY_OTT_TOKEN; ottRetryAttempt++) {
            try {
                body = restTemplate.exchange(url, HttpMethod.POST, request, LogoutUserResponse.class).getBody();
            } catch (HttpStatusCodeException e) {
                logger.error("[** ANGELONE LOGOUT USER ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE LOGOUT USER ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE LOGOUT USER ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessToken = AngelOneErrorCodeConstants.LOGOUT_USER_FAILED;
                exception = e;
            } catch (Exception e) {
                logger.error("[** ANGELONE LOGOUT USER ERROR **] for USER ID := {}> Exception : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE LOGOUT USER FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE LOGOUT USER ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessToken = AngelOneErrorCodeConstants.LOGOUT_USER_FAILED;
                exception = e;
            }
            if (body != null && !body.getStatus()) {
                logger.info("[** ANGELONE LOGOUT USER ERROR **] for USER ID := {} > Fault : {}", userResponse.getUserId(), body.getMessage());
                getAcessToken = AngelOneErrorCodeConstants.LOGOUT_USER_FAILED;
                getAcessToken.setMessage(body.getMessage());
                String message = "ANGELONE LOGOUT USER FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE LOGOUT USER ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
            }
            if (Objects.nonNull(body) && body.getStatus()) {
                break;
            }
        }
        logger.info("******* [AngelOneClientImpl][LOGOUT USER] Completed for USER ID  := {}", userResponse.getUserId());
        return body;
    }


    @Override
    public void logoutAllActiveUsers() {
        logger.info("******* [AngelOneClientImpl][LOGOUT ALL USER] Completed");
        userService.getAllActiveUsersByBroker(BrokerEnum.ANGEL_ONE.toString()).forEach(angelOneUser -> {
            logoutUser(angelOneUser.getAccessToken(), angelOneUser);
        });
        logger.info("******* [AngelOneClientImpl][LOGOUT ALL USER] Completed");
    }

    @Override
    public GetCandleResponse getCandleData(String currentAccessToken, UserResponse userResponse, GetCandleRequest getCandleRequest) {
        logger.info("******* [AngelOneClientImpl][GET CANDLE DATA] called for USER ID  := {}", userResponse.getUserId());
        final HttpHeaders headers = new HttpHeaders();
        GetAccessTokenResponse systemUserAccessToken = getUserAccessToken(userResponse);
        setCommonHeaders(headers, userResponse);
        if (systemUserAccessToken == null) {
            throw new AngelOneValidationException(AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_EMPTY);
        }
        LoginData data = systemUserAccessToken.getData();
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(data.getJwtToken()));
        HttpEntity<GetCandleRequest> request = new HttpEntity<>(getCandleRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.GET_CANDLE_DATA;
        GetCandleResponse body = null;
        BusinessError getAcessToken;
        for (int ottRetryAttempt = 0; ottRetryAttempt < Constants.MAX_RETRY_OTT_TOKEN; ottRetryAttempt++) {
            try {
                body = restTemplate.exchange(url, HttpMethod.POST, request, GetCandleResponse.class).getBody();
            } catch (HttpStatusCodeException e) {
                logger.error("[** ANGELONE GET CANDLE DATA ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE GET CANDLE DATA ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE GET CANDLE DATA ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessToken = AngelOneErrorCodeConstants.GET_CANDLE_DATA_FAILED;
            } catch (Exception e) {
                logger.error("[** ANGELONE GET CANDLE DATA ERROR **] for USER ID := {}> Exception : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE GET CANDLE DATA FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE GET CANDLE DATA ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
                getAcessToken = AngelOneErrorCodeConstants.GET_CANDLE_DATA_FAILED;
            }
            if (body != null && !body.getStatus()) {
                logger.info("[** ANGELONE GET CANDLE DATA ERROR **] for USER ID := {} > Fault : {}", userResponse.getUserId(), body.getMessage());
                getAcessToken = AngelOneErrorCodeConstants.GET_CANDLE_DATA_FAILED;
                getAcessToken.setMessage(body.getMessage());
                String message = "ANGELONE GET CANDLE DATA FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE GET CANDLE DATA ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_ACCESS_TOKEN, message, errorMessage);
            }
            if (Objects.nonNull(body) && body.getStatus()) {
                break;
            }
        }
        logger.info("******* [AngelOneClientImpl][GET CANDLE DATA] Completed for USER ID  := {}", userResponse.getUserId());
        return body;
    }


    @Override
    public QouteResponse getCandleData(String currentAccessToken, UserResponse userResponse, GetLtpRequest getLtpRequest) {
        logger.info("******* [AngelOneClientImpl][GET CANDLE DATA] called for Instrument Token  := {}, Instrument Name {}", getLtpRequest.getSymboltoken(), getLtpRequest.getTradingsymbol());
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userResponse);
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(currentAccessToken));
        HttpEntity<GetLtpRequest> request = new HttpEntity<>(getLtpRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.GET_CANDLE_DATA;
        QouteResponse body = null;
        BusinessError businessError;
        for (int ottRetryAttempt = 0; ottRetryAttempt < AngelOneConstants.MAX_RETRY_CANDLE_DATA; ottRetryAttempt++) {
            try {
                body = restTemplate.exchange(url, HttpMethod.POST, request, QouteResponse.class).getBody();
            } catch (HttpStatusCodeException e) {
                logger.error("[** ANGELONE GET CANDLE DATA ERROR **] for USER ID := {} > HttpStatusCodeException : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE GET CANDLE DATA ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE GET CANDLE DATA ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_CANDLE_DATA, message, errorMessage);
                businessError = AngelOneErrorCodeConstants.GET_CANDLE_DATA_FAILED;
            } catch (Exception e) {
                logger.error("[** ANGELONE GET CANDLE DATA ERROR **] for USER ID := {}> Exception : {}", userResponse.getUserId(), e.getMessage());
                String message = "ANGELONE GET CANDLE DATA FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE GET CANDLE DATA ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_CANDLE_DATA, message, errorMessage);
                businessError = AngelOneErrorCodeConstants.GET_CANDLE_DATA_FAILED;
            }
            if (body != null && !body.getStatus()) {
                logger.info("[** ANGELONE GET CANDLE DATA ERROR **] for USER ID := {} > Fault : {}", userResponse.getUserId(), body.getMessage());
                businessError = AngelOneErrorCodeConstants.GET_CANDLE_DATA_FAILED;
                businessError.setMessage(body.getMessage());
                String message = "ANGELONE GET CANDLE DATA FAILED ,RETRYING RETRY ATTEMPT := {} ";
                String errorMessage = "[** ANGELONE GET CANDLE DATA ERROR **] InterruptedException : {}";
                waitForNextRetryAttempt(ottRetryAttempt, AngelOneConstants.MAX_RETRY_CANDLE_DATA, message, errorMessage);
            }
            if (Objects.nonNull(body) && body.getStatus()) {
                break;
            }
        }
        logger.info("******* [AngelOneClientImpl][GET CANDLE DATA] Completed for Instrument Token  := {}, Instrument Name {}", getLtpRequest.getSymboltoken(), getLtpRequest.getTradingsymbol());
        return body;
    }


    @Override
    public GetAccessTokenResponse getAccessTokenSystemUser() {
        UserResponse systemUserByBroker = userService.getActiveBrokerSystemUser(BrokerEnum.ANGEL_ONE.toString());
        return getUserAccessToken(systemUserByBroker);
    }


    public GetAccessTokenResponse getUserAccessToken(UserResponse userResponse) {
        GetAccessTokenResponse cacheResponse = getAngelOneUserTokenFromCache(userResponse);
        if (Objects.nonNull(cacheResponse)) {
            return cacheResponse;
        }
        GetAccessTokenResponse accessToken = getAccessToken(userResponse);
        if (accessToken != null) {
            // Need to update User AccessToken
            LoginData accessDetails = accessToken.getData();
            //if (accessDetails != null) {
            userService.updateAccessDetails(accessToken.getUserId(),
                    accessDetails.getJwtToken(), accessDetails.getRefreshToken(), accessDetails.getFeedToken());
     /*       }else{
                throw new AlgotradeException(ErrorCodeConstants.INVALID_ACCESS_TOKEN);
            }*/
            return accessToken;
        }
        return null;
    }


    @Override
    public QouteResponse getQuote(ExchangeType exchangeType, List<String> instruments) {
        logger.info("ANGELONE QUOTE for INSTRUMENTS := {}  Called ", instruments);
        UserResponse systemUser;
        GetAccessTokenResponse systemUserAccessToken;
        synchronized (this) {
            systemUser = userService.getActiveBrokerSystemUser(BrokerEnum.ANGEL_ONE.toString());
            systemUserAccessToken = getUserAccessToken(systemUser);
        }
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, systemUser);
        LoginData data = systemUserAccessToken.getData();
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(data.getJwtToken()));
        if (systemUserAccessToken == null) {
            throw new AngelOneValidationException(AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_EMPTY);
        }
        QuoteRequest quoteRequest = buildQuoteRequest(instruments);
        HttpEntity<QuoteRequest> request = new HttpEntity<>(quoteRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.GET_QUOTE_URL;
        QouteResponse body = null;
        BusinessError getLtpBusinessError = null;
        for (int wsRetryAttempt = 0; wsRetryAttempt < WebSocketConstant.MAX_RETRY_LTP; wsRetryAttempt++) {
            getLtpBusinessError = null;
            try {
                synchronized (this) {
                    body = restTemplate.exchange(url, HttpMethod.POST, request, QouteResponse.class).getBody();
                }
            } catch (HttpStatusCodeException e) {
                logger.error("ANGELONE QUOTE for INSTRUMENTS := {} > HttpStatusCodeException : {}", instruments, e.getMessage());
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    logger.info("For Unauthorized scenario fetching session token again for Instruments {} before retry", instruments);
                    systemUserAccessToken = getAccessToken(systemUser);
                    headers.set("Authorization", "Bearer " + systemUserAccessToken.getData().getJwtToken());
                    request = new HttpEntity<>(headers);
                }
                String message = "ANGELONE QUOTE ,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE QUOTE InterruptedException : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            } catch (Exception e) {
                logger.error("ANGELONE QUOTE for INSTRUMENTS := {} > Exception : {}", instruments, e.getMessage());
                String message = "ANGELONE QUOTE for INSTRUMENTS,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE QUOTE for INSTRUMENTS Exception : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            }
            break;
        }

        if (Objects.nonNull(getLtpBusinessError)) {
            logger.error("ANGELONE QUOTE for INSTRUMENTS := {} > FAILED", instruments);
        }
        logger.info("ANGELONE QUOTE for INSTRUMENTS := {}  Completed ", instruments);
        return body;
    }

    private static QuoteRequest buildQuoteRequest(List<String> instruments) {
        Map<String, List<String>> exchangeTokens = new HashMap<>();
        exchangeTokens.put(ExchangeType.NSE.toString(), instruments);
        QuoteRequest quoteRequest = QuoteRequest.builder()
                .mode(QuoteMode.FULL.toString())
                .exchangeTokens(exchangeTokens)
                .build();
        return quoteRequest;

    }

    private static GetAccessTokenResponse getAngelOneUserTokenFromCache(UserResponse userResponse) {
        if (StringUtils.isNoneBlank(userResponse.getSessionToken())
                && userResponse.getLastLogin() != null) {
            LocalDate lastLoginDate = userResponse.getLastLogin().toLocalDate();
            LocalDate currentDate = DateUtils.getCurrentDateTimeIst().toLocalDate();
            logger.info("ANGELONE USER TOKEN CACHE USER ID := {} > LAST LOGIN DATE  : {}, CURRENT DATE {}", userResponse.getUserId(), lastLoginDate, currentDate);
            if (DateUtils.isSameDayLocalDate(lastLoginDate, currentDate)) {
                return GetAccessTokenResponse.builder().UserId(userResponse.getUserId())
                        .data(LoginData.builder().jwtToken(userResponse.getSessionToken())
                                .feedToken(userResponse.getFeedToken())
                                .refreshToken(userResponse.getRefreshToken())
                                .build())
                        .build();
            }
        }
        return null;
    }

    public AngelOrderResponse placeOrder(AngelOrderRequest angelOrderRequest, UserResponse userDetails) {
        logger.debug("ANGELONE place order Called ");
        GetAccessTokenResponse systemUserAccessToken = getUserAccessToken(userDetails);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails);
        LoginData data = systemUserAccessToken.getData();
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(data.getJwtToken()));
        if (systemUserAccessToken == null) {
            throw new AngelOneValidationException(AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_EMPTY);
        }
        HttpEntity<AngelOrderRequest> request = new HttpEntity<>(angelOrderRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.PLACE_ORDER_URL;
        AngelOrderResponse body = null;
        BusinessError getLtpBusinessError = null;
        for (int wsRetryAttempt = 0; wsRetryAttempt < WebSocketConstant.MAX_RETRY_LTP; wsRetryAttempt++) {
            getLtpBusinessError = null;
            try {
                if (userDetails.getIsRealTradingEnabled() && !commonUtils.getOffMarketHoursTestFlag() && !angelOrderRequest.isMockOrder()) {
                    body = restTemplate.exchange(url, HttpMethod.POST, request, AngelOrderResponse.class).getBody();
                } else {
                    body = mockRestCall(angelOrderRequest);
                }
            } catch (HttpStatusCodeException e) {
                logger.error("ANGELONE place order > HttpStatusCodeException : {}", e.getMessage());
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    logger.info("For Unauthorized scenario fetching session token again for place order before retry");
                    systemUserAccessToken = getAccessToken(userDetails);
                    headers.set("Authorization", "Bearer " + systemUserAccessToken.getData().getJwtToken());
                    request = new HttpEntity<>(headers);
                }
                String message = "ANGELONE place order ,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE place order InterruptedException : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            } catch (Exception e) {
                logger.error("ANGELONE place order > Exception : {}", e.getMessage());
                String message = "ANGELONE place order for INSTRUMENTS,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE place order for INSTRUMENTS Exception : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            }
            break;
        }

        if (Objects.nonNull(getLtpBusinessError)) {
            logger.error("ANGELONE USER ID := {} : place order > FAILED", userDetails.getUserId());
        }
        logger.info("ANGELONE USER ID := {} : ORDER PLACED AT BROKER", userDetails.getUserId());
        return body;
    }

    public AngelOrderResponse modifyOrder(AngelOrderRequest angelOrderRequest, String userId) {
        logger.info("ANGELONE modify order Called ");
        UserResponse userDetails = userService.getUserById(userId);
        GetAccessTokenResponse systemUserAccessToken = getUserAccessToken(userDetails);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails);
        LoginData data = systemUserAccessToken.getData();
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(data.getJwtToken()));
        if (systemUserAccessToken == null) {
            throw new AngelOneValidationException(AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_EMPTY);
        }
        HttpEntity<AngelOrderRequest> request = new HttpEntity<>(angelOrderRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.MODIFY_ORDER_URL;
        AngelOrderResponse body = null;
        BusinessError getLtpBusinessError = null;
        for (int wsRetryAttempt = 0; wsRetryAttempt < WebSocketConstant.MAX_RETRY_LTP; wsRetryAttempt++) {
            getLtpBusinessError = null;
            try {
                if (userDetails.getIsRealTradingEnabled() && !commonUtils.getOffMarketHoursTestFlag()) {
                    body = restTemplate.exchange(url, HttpMethod.POST, request, AngelOrderResponse.class).getBody();
                } else {
                    body = mockRestCall(angelOrderRequest);
                }
            } catch (HttpStatusCodeException e) {
                logger.error("ANGELONE modify order > HttpStatusCodeException : {}", e.getMessage());
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    logger.info("For Unauthorized scenario fetching session token again for modify order before retry");
                    systemUserAccessToken = getAccessToken(userDetails);
                    headers.set("Authorization", "Bearer " + systemUserAccessToken.getData().getJwtToken());
                    request = new HttpEntity<>(headers);
                }
                String message = "ANGELONE modify order ,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE modify order InterruptedException : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            } catch (Exception e) {
                logger.error("ANGELONE modify order > Exception : {}", e.getMessage());
                String message = "ANGELONE modify order for INSTRUMENTS,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE modify order for INSTRUMENTS Exception : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            }
            break;
        }

        if (Objects.nonNull(getLtpBusinessError)) {
            logger.error("ANGELONE USER ID := {} : modify order > FAILED", userDetails.getUserId());
        }
        logger.info("ANGELONE USER ID := {} : ORDER MODIFIED AT KOTAK", userDetails.getUserId());
        return body;
    }

    public AngelOrderResponse cancelOrder(Long orderId, String userId) {
        logger.info("ANGELONE cancel order Called ");
        UserResponse userDetails = userService.getUserById(userId);
        GetAccessTokenResponse systemUserAccessToken = getUserAccessToken(userDetails);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails);
        LoginData data = systemUserAccessToken.getData();
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(data.getJwtToken()));
        if (systemUserAccessToken == null) {
            throw new AngelOneValidationException(AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_EMPTY);
        }
        AngelOrderRequest angelOrderRequest = new AngelOrderRequest();
        angelOrderRequest.setVariety(VarietyEnum.NORMAL);
        angelOrderRequest.setOrderId(orderId);
        HttpEntity<AngelOrderRequest> request = new HttpEntity<>(angelOrderRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.MODIFY_ORDER_URL;
        AngelOrderResponse body = null;
        BusinessError getLtpBusinessError = null;
        for (int wsRetryAttempt = 0; wsRetryAttempt < WebSocketConstant.MAX_RETRY_LTP; wsRetryAttempt++) {
            getLtpBusinessError = null;
            try {
                if (userDetails.getIsRealTradingEnabled() && !commonUtils.getOffMarketHoursTestFlag()) {
                    body = restTemplate.exchange(url, HttpMethod.POST, request, AngelOrderResponse.class).getBody();
                }
            } catch (HttpStatusCodeException e) {
                logger.error("ANGELONE cancel order > HttpStatusCodeException : {}", e.getMessage());
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    logger.info("For Unauthorized scenario fetching session token again for cancel order before retry");
                    systemUserAccessToken = getAccessToken(userDetails);
                    headers.set("Authorization", "Bearer " + systemUserAccessToken.getData().getJwtToken());
                    request = new HttpEntity<>(headers);
                }
                String message = "ANGELONE cancel order ,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE cancel order InterruptedException : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            } catch (Exception e) {
                logger.error("ANGELONE cancel order > Exception : {}", e.getMessage());
                String message = "ANGELONE cancel order for INSTRUMENTS,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE cancel order for INSTRUMENTS Exception : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            }
            break;
        }

        if (Objects.nonNull(getLtpBusinessError)) {
            logger.error("ANGELONE USER ID := {} : cancel order > FAILED", userDetails.getUserId());
        }
        logger.info("ANGELONE USER ID := {} : ORDER CANCELED AT KOTAK", userDetails.getUserId());
        return body;
    }

    @Override
    public AngelAllOrdersResponse getAllOrders(String userId) {
        logger.info("ANGELONE get orders Called ");
        UserResponse userDetails = userService.getUserById(userId);
        GetAccessTokenResponse systemUserAccessToken = getUserAccessToken(userDetails);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails);
        LoginData data = systemUserAccessToken.getData();
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(data.getJwtToken()));
        if (systemUserAccessToken == null) {
            throw new AngelOneValidationException(AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_EMPTY);
        }
        HttpEntity<AngelOrderRequest> request = new HttpEntity<>(headers);
        String url = getUrl() + AngelOneRouteConstants.GET_ORDERS_URL;
        AngelAllOrdersResponse body = null;
        BusinessError getLtpBusinessError = null;
        getLtpBusinessError = null;
        try {
            body = restTemplate.exchange(url, HttpMethod.GET, request, AngelAllOrdersResponse.class).getBody();
        } catch (HttpStatusCodeException e) {
            logger.error("ANGELONE get orders > HttpStatusCodeException : {}", e.getMessage());
            if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                logger.info("For Unauthorized scenario fetching session token again for cancel order before retry");
                systemUserAccessToken = getAccessToken(userDetails);
                headers.set("Authorization", "Bearer " + systemUserAccessToken.getData().getJwtToken());
                request = new HttpEntity<>(headers);
            }
            String message = "ANGELONE get orders ,RETRYING RETRY ATTEMPT := {0} ";
            String errorMessage = "ANGELONE get orders InterruptedException : {0}";
//                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
            getLtpBusinessError = AngelOneErrorCodeConstants.GET_ORDERS_ERROR;
//                continue;
        } catch (Exception e) {
            logger.error("ANGELONE get orders > Exception : {}", e.getMessage());
            String message = "ANGELONE get orders for INSTRUMENTS,RETRYING RETRY ATTEMPT := {0} ";
            String errorMessage = "ANGELONE get orders for INSTRUMENTS Exception : {0}";
//                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
            getLtpBusinessError = AngelOneErrorCodeConstants.GET_ORDERS_ERROR;
//                continue;
        }
//            break;

        if (Objects.nonNull(getLtpBusinessError)) {
            logger.error("ANGELONE USER ID := {} : get order > FAILED", userDetails.getUserId());
        }
        logger.info("ANGELONE USER ID := {} : get all orders complete", userDetails.getUserId());
        return body;
    }

    private AngelOrderResponse mockRestCall(AngelOrderRequest order) {
        logger.info("******* [AngelOneClientImpl] Mocking place order request.");
        AngelOrderResponse body;
        long number = (long) Math.floor(Math.random() * 9000000000000L) + 1000000000000L;
        String uniqueOrderId = RandomStringUtils.randomAlphanumeric(12);
        OrderDetailDto orderDetailDto = new OrderDetailDto();
        orderDetailDto.setOrderId(number);
        orderDetailDto.setUniqueOrderId(uniqueOrderId);
        body = new AngelOrderResponse();
        body.setMessage("Your mock order has been Placed and Forwarded to the Exchange:" + number);
        body.setStatus("Success");
        body.setData(orderDetailDto);
        return body;
    }


    public AngelOrderResponse placeGttOrder(AngelOrderRequest angelOrderRequest, UserResponse userDetails) {
        logger.info("ANGELONE place order Called ");
        GetAccessTokenResponse systemUserAccessToken = getUserAccessToken(userDetails);
        final HttpHeaders headers = new HttpHeaders();
        setCommonHeaders(headers, userDetails);
        LoginData data = systemUserAccessToken.getData();
        headers.set(AngelOneConstants.AUTHORIZATION_HEADER, AngelOneConstants.AUTHORIZATION_HEADER_BEARER.concat(data.getJwtToken()));
        if (systemUserAccessToken == null) {
            throw new AngelOneValidationException(AngelOneErrorCodeConstants.GET_ACCESS_TOKEN_EMPTY);
        }
        HttpEntity<AngelOrderRequest> request = new HttpEntity<>(angelOrderRequest, headers);
        String url = getUrl() + AngelOneRouteConstants.PLACE_ORDER_URL;
        AngelOrderResponse body = null;
        BusinessError getLtpBusinessError = null;
        for (int wsRetryAttempt = 0; wsRetryAttempt < WebSocketConstant.MAX_RETRY_LTP; wsRetryAttempt++) {
            getLtpBusinessError = null;
            try {
                if (userDetails.getIsRealTradingEnabled() && !commonUtils.getOffMarketHoursTestFlag()) {
                    body = restTemplate.exchange(url, HttpMethod.POST, request, AngelOrderResponse.class).getBody();
                } else {
                    body = mockRestCall(angelOrderRequest);
                }
            } catch (HttpStatusCodeException e) {
                logger.error("ANGELONE place order > HttpStatusCodeException : {}", e.getMessage());
                if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                    logger.info("For Unauthorized scenario fetching session token again for place order before retry");
                    systemUserAccessToken = getAccessToken(userDetails);
                    headers.set("Authorization", "Bearer " + systemUserAccessToken.getData().getJwtToken());
                    request = new HttpEntity<>(headers);
                }
                String message = "ANGELONE place order ,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE place order InterruptedException : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            } catch (Exception e) {
                logger.error("ANGELONE place order > Exception : {}", e.getMessage());
                String message = "ANGELONE place order for INSTRUMENTS,RETRYING RETRY ATTEMPT := {0} ";
                String errorMessage = "ANGELONE place order for INSTRUMENTS Exception : {0}";
                waitForNextRetryAttempt(wsRetryAttempt, Constants.MAX_RETRY_PLACE_ORDER, message, errorMessage);
                getLtpBusinessError = AngelOneErrorCodeConstants.GET_QUOTE_ERROR;
                continue;
            }
            break;
        }

        if (Objects.nonNull(getLtpBusinessError)) {
            logger.error("ANGELONE USER ID := {} : place order > FAILED", userDetails.getUserId());
        }
        logger.info("ANGELONE USER ID := {} : ORDER PLACED AT BROKER", userDetails.getUserId());
        return body;
    }
}