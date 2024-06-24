package com.trade.algotrade.client.angelone.websocket.ticker;

import com.trade.algotrade.client.angelone.websocket.models.*;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.dto.websocket.LiveFeedWSMessage;
import com.trade.algotrade.enums.MessageSource;
import com.trade.algotrade.service.InstrumentService;
import com.trade.algotrade.service.ManualSchedulerService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import com.trade.algotrade.utils.WebsocketUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Component
public class SmartStreamListenerImpl implements SmartStreamListener {

    private final Logger logger = LoggerFactory.getLogger(SmartStreamListenerImpl.class);

    @Autowired
    ManualSchedulerService manualSchedulerService;

    @Autowired
    WebsocketUtils websocketUtils;

    @Autowired
    InstrumentService instrumentService;

    @Autowired
    CommonUtils commonUtils;

    @Override
    public void onLTPArrival(LTP ltp) {

        if (Objects.isNull(ltp) || Objects.isNull(ltp.getToken()) || StringUtils.isEmpty(ltp.getToken().getToken())) {
            return;
        }
        TradeUtils.isWebsocketConnected = true;
//        Long instrumentToken = Long.valueOf(ltp.getToken().getToken().replaceAll("[^-?0-9]+", ""));
//        Long kotakInstrumentToken = null;
//        try {
//            kotakInstrumentToken = instrumentService.getKotakInstrumentByAngel(instrumentToken);
//        } catch (Exception e) {
//            logger.error("Exception while fetching {}", e.getMessage());
//        }
        Long finalKotakInstrumentToken =  Long.valueOf(ltp.getToken().getToken().replaceAll("[^-?0-9]+", ""));
        Optional<Long> optionalLong = TradeUtils.activeInstruments.keySet().stream().filter(aLong -> aLong.equals(finalKotakInstrumentToken)).findFirst();
        if (optionalLong.isEmpty() && !Long.valueOf(commonUtils.getConfigValue(ConfigConstants.KOTAK_NIFTY_BANK_INSTRUMENT)).equals(finalKotakInstrumentToken)) {
            logger.debug("Active instruments not present hence returning");
            return;
        }
        BigDecimal mappedLtp = convertPriceToBigdecimal(ltp.getLastTradedPrice());
        if (ltp.getExchangeType().equals(ExchangeType.NSE_FO)) {
            logger.info("LTP token = {} and price = {}", finalKotakInstrumentToken, mappedLtp);
        }
        LiveFeedWSMessage liveFeedWSMessage = LiveFeedWSMessage.builder()
                .instrumentToken(finalKotakInstrumentToken)
                .ltp(mappedLtp)
                .instrumentName(ltp.getExchangeType().equals(ExchangeType.NSE_CM) ? Constants.BANK_NIFTY_INDEX : Constants.BANK_NIFTY)
                .timeStamp(DateUtils.epochTimeToString(ltp.getExchangeFeedTimeEpochMillis()))
                .messageSource(MessageSource.WEBSOCKET).build();
        manualSchedulerService.processLtpMessage(liveFeedWSMessage);
    }

    private BigDecimal convertPriceToBigdecimal(long price) {
        String ltpString = String.valueOf(price);
        String wholeLtp = ltpString.substring(0, ltpString.length() - 2);
        String decimalLtp = ltpString.substring(Math.max(ltpString.length() - 2, 0));
        return new BigDecimal(wholeLtp.concat(".").concat(decimalLtp));
    }

    @Override
    public void onQuoteArrival(Quote quote) {

    }

    @Override
    public void onSnapQuoteArrival(SnapQuote snapQuote) {

    }

    @Override
    public void onDepthArrival(Depth depth) {

    }

    @Override
    public void onConnected() {
        logger.info(" ******** Smart stream listener connected");
        TradeUtils.isWebsocketConnected = true;
    }

    @Override
    public void onDisconnected() {
        logger.info(" ******** Smart stream listener disconnected");
        TradeUtils.isWebsocketConnected = false;
    }

    @Override
    public void onError(SmartStreamError error) {
        logger.error("Error in smart stream ticker {}", error.getException());
        logger.error("Error in smart stream ticker message {}", error.getException().getMessage());
    }

    @Override
    public void onPong() {

    }

    @Override
    public SmartStreamError onErrorCustom() {
        return null;
    }
}
