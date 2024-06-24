package com.trade.algotrade.chartpatterns;

import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.enums.BrokerEnum;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.NotificationService;
import com.trade.algotrade.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

@Component
public class ChartPatternDetectorService {

    private final Logger logger = LoggerFactory.getLogger(ChartPatternDetectorService.class);
    @Autowired
    UserService userService;
    @Autowired
    NotificationService notificationService;

    @Autowired
    SingleCandleStickPattern singleCandleStickPattern;

    @Autowired
    DoubleCandleStickPattern doubleCandleStickPattern;


    @Autowired
    TripleCandleStickPattern tripleCandleStickPattern;

    public void detectPatternAndSendNotification(List<CandleEntity> candles) {
        Optional<UserResponse> userResponseOptional = userService.getAllActiveUsersByBroker(BrokerEnum.KOTAK_SECURITIES.toString()).stream().filter(userResponse -> userResponse.getSystemUser()).findFirst();
        if (userResponseOptional.isPresent()) {
            if (!CollectionUtils.isEmpty(candles)) {
                String message = null;
                if (singleCandleStickPattern.isBullishDoji()) {
                    message = "BULLISH DOJI DETECTED";
                }
                if (singleCandleStickPattern.isBearishDoji()) {
                    message = "BEARISH DOJI DETECTED";
                }

                if (singleCandleStickPattern.isBullishHammer()) {
                    message = "BULLISH HAMMER DETECTED";
                }
                if (singleCandleStickPattern.isBearishShootingStar()) {
                    message = "BEARISH SHOOTING START DETECTED";
                }

                notificationService.sendTelegramNotification(userResponseOptional.get().getTelegramChatId(), message);

                if (tripleCandleStickPattern.isBearishEveningStar()) {
                    message = "EVENING STAR PATTERN DETECTED, MARKET  WILL BE BEARISH";
                }

                if (tripleCandleStickPattern.isBullishMorningStar()) {
                    message = "MORNING STAR PATTERN DETECTED, MARKET  WILL BE BULLISH";
                }
                notificationService.sendTelegramNotification(userResponseOptional.get().getTelegramChatId(), message);

                if (doubleCandleStickPattern.isBearishEngolfer()) {
                    message = "BULLISH ENGULFING PATTERN DETECTED, MARKET  WILL BE BEARISH";
                }

                if (doubleCandleStickPattern.isBullishEngolfer()) {
                    message = "BULLISH ENGULFING PATTERN DETECTED, MARKET  WILL BE BULLISH";
                }
                notificationService.sendTelegramNotification(userResponseOptional.get().getTelegramChatId(), message);

            }
        }
    }
}
	
	