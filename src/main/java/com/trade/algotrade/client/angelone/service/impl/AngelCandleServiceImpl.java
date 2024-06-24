package com.trade.algotrade.client.angelone.service.impl;

import com.trade.algotrade.client.angelone.AngelOneClient;
import com.trade.algotrade.client.angelone.enums.AngelOneExchange;
import com.trade.algotrade.client.angelone.enums.CandleTimeFrame;
import com.trade.algotrade.client.angelone.request.GetCandleRequest;
import com.trade.algotrade.client.angelone.response.historical.GetCandleResponse;
import com.trade.algotrade.client.angelone.service.AngelCandleService;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.dto.BigCandle;
import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.enitiy.KotakInstrumentMasterEntity;
import com.trade.algotrade.enums.BrokerEnum;
import com.trade.algotrade.enums.CandleColor;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.AlgoConfigurationService;
import com.trade.algotrade.service.InstrumentService;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
public class AngelCandleServiceImpl implements AngelCandleService {

    private static final Logger logger = LoggerFactory.getLogger(AngelCandleServiceImpl.class);


    @Autowired
    AngelOneClient angelOneClient;


    @Autowired
    UserService userService;

    @Autowired
    AlgoConfigurationService algoConfigurationService;

    @Autowired
    InstrumentService instrumentService;

    private static List<BigCandle> bigCandle = new ArrayList<>();


    @Autowired
    CommonUtils commonUtils;

    @Override
    public List<CandleEntity> getCandleData(GetCandleRequest candleRequest) {
        UserResponse systemUser = userService.getActiveBrokerSystemUser(BrokerEnum.ANGEL_ONE.toString());
        if (candleRequest == null) {
            candleRequest = GetCandleRequest.builder().exchange(AngelOneExchange.NSE.toString())
                    .fromdate(DateUtils.getCurrentDateTimeIst().format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT)))
                    .interval(CandleTimeFrame.FIFTEEN_MINUTE.toString()).build();
        }
        GetCandleResponse candleData = angelOneClient.getCandleData(systemUser.getAccessToken(), systemUser, candleRequest);
        List<CandleEntity> candleEntityList = new ArrayList<>();
        if (candleData != null && candleData.getStatus()) {
            List<List<Object>> candles = candleData.getData();

            if (!CollectionUtils.isEmpty(candles)) {
                for (List<Object> candle : candles) {
                    CandleEntity candleEntity = new CandleEntity();
                    candleEntity.setCandleStart((String) candle.get(0));
                    candleEntity.setOpen(new BigDecimal((Double) candle.get(1)));
                    candleEntity.setHigh(new BigDecimal((Double) candle.get(2)));
                    candleEntity.setLow(new BigDecimal((Double) candle.get(3)));
                    candleEntity.setClose(new BigDecimal((Double) candle.get(4)));
                    if (candleEntity.getOpen() != null && candleEntity.getClose() != null && candleEntity.getOpen().compareTo(candleEntity.getClose()) == 1) {
                        candleEntity.setColor(CandleColor.RED);
                    } else {
                        candleEntity.setColor(CandleColor.GREEN);
                    }
                    candleEntity.setCandleBodyPoints(candleEntity.getClose().subtract(candleEntity.getOpen()));
                    candleEntity.setCandleTotalPoints(candleEntity.getHigh().subtract(candleEntity.getLow()));
                    candleEntityList.add(candleEntity);
                }
            }
        }

        // TODO Sorting should be added by default decending
        return candleEntityList;
    }

    public BigDecimal latestTrendFinder() {
        if (CommonUtils.getOffMarketHoursTestFlag()) {
            return new BigDecimal(102);
        }
        if (!DateUtils.isBeforeTime(commonUtils.getConfigValue(ConfigConstants.BIG_CANDLE_TRADING_START_TIME)) || commonUtils.isTodayExpiryDay()) {
            if (TradeUtils.isLatestTrendFinderActive) {
                String bigCandleTimeFrame = algoConfigurationService.getConfigurationDetails().getConfigurationDetails().getConfigs().get(Constants.BIG_CANDLE_TREND_FINDER_TIME_GAP);
                Integer timeInMunutes = Integer.valueOf(bigCandleTimeFrame);
                GetCandleRequest getCandleRequest = GetCandleRequest.builder().exchange(AngelOneExchange.NSE.toString()).fromdate(DateUtils.getCurrentDateTimeIst().format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT))).interval(getCandletTimeFrame(timeInMunutes).toString()).build();
                List<CandleEntity> candleData = getCandleData(getCandleRequest);
                BigDecimal trendValue = candleData.get(0).getHigh().subtract(candleData.get(0).getLow());
                logger.info("********* BANK NIFTY For Last {} Minutes the current Trend value :{}", timeInMunutes, trendValue);
                return trendValue;
            } else {
                logger.debug("Already open orders are present for all user, hence not finding latest trend.");
                return null;
            }
        }
        return null;
    }

    CandleTimeFrame getCandletTimeFrame(Integer timeInMunutes) {
        CandleTimeFrame candleTimeFrame = CandleTimeFrame.FIFTEEN_MINUTE;
        switch (timeInMunutes) {
            case 10:
                candleTimeFrame = CandleTimeFrame.TEN_MINUTE;
                break;
            case 15:
                candleTimeFrame = candleTimeFrame;
                break;
            case 30:
                candleTimeFrame = CandleTimeFrame.THIRTY_MINUTE;
                break;
            case 60:
                candleTimeFrame = CandleTimeFrame.ONE_HOUR;
                break;
            case 60 * 24:
                candleTimeFrame = CandleTimeFrame.ONE_DAY;
                break;
            default:
                break;
        }
        return candleTimeFrame;
    }

    @Override
    public Collection<CandleEntity> findCandlesBySymbolsDescending(String bankNiftyIndex) {
        AngelOneInstrumentMasterEntity nifyBankInstrument = instrumentService.findByInstrumentName(bankNiftyIndex);
        GetCandleRequest candleRequest = GetCandleRequest.builder().exchange(AngelOneExchange.NSE.toString())
                .fromdate(DateUtils.getCurrentDateTimeIst().format(DateTimeFormatter.ofPattern(DateUtils.ANGEL_REQUEST_FORMAT)))
                .symboltoken(nifyBankInstrument.getInstrumentToken().toString())
                .interval(CandleTimeFrame.FIFTEEN_MINUTE.toString())
                .build();

        List<CandleEntity> candleData = getCandleData(candleRequest);
        if (!CollectionUtils.isEmpty(candleData)) {
            return (Collection<CandleEntity>) candleData.stream().sorted(new Comparator<CandleEntity>() {
                // TODO This sorting logic needs to be revisited
                @Override
                public int compare(CandleEntity o1, CandleEntity o2) {
                    return o2.getCandleStart().compareTo(o1.getCandleStart());
                }
            });
        }
        return new ArrayList<>();
    }


    @Override
    public void clearTrendData() {
        bigCandle = new ArrayList<>();
    }

}