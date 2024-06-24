package com.trade.algotrade.chartpatterns;

import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.utils.TradeUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

public class CandleStickPattern {

    List<CandleEntity> candles;

    public static Boolean isBearishEngulfingIndicator(List<CandleEntity> candles) {
        if (CollectionUtils.isEmpty(candles) || candles.size() < 2) {
            // Engulfing is a 2-candle pattern
            return false;
        }
        int index = candles.size() - 1;
        CandleEntity prevCandleEntity = candles.get(index - 1);
        CandleEntity currCandleEntity = candles.get(index);
        if (prevCandleEntity.isBullish() && currCandleEntity.isBearish()) {
            final BigDecimal prevOpenPrice = prevCandleEntity.getOpen();
            final BigDecimal prevClosePrice = prevCandleEntity.getClose();
            final BigDecimal currOpenPrice = currCandleEntity.getOpen();
            final BigDecimal currClosePrice = currCandleEntity.getClose();
            return TradeUtils.isGreaterThan(currOpenPrice, prevOpenPrice) && TradeUtils.isGreaterThan(currOpenPrice, prevClosePrice)
                    && TradeUtils.isLessThan(currClosePrice, prevOpenPrice) && TradeUtils.isLessThan(currClosePrice, prevClosePrice);
        }
        return false;
    }

    public static Boolean isBullishHaramiIndicator(List<CandleEntity> candles) {
        if (CollectionUtils.isEmpty(candles) || candles.size() < 2) {
            // Engulfing is a 2-candle pattern
            return false;
        }
        int index = candles.size() - 1;
        CandleEntity prevCandleEntity = candles.get(index - 1);
        CandleEntity currCandleEntity = candles.get(index);
        if (prevCandleEntity.isBearish() && currCandleEntity.isBullish()) {
            final BigDecimal prevOpenPrice = prevCandleEntity.getOpen();
            final BigDecimal prevClosePrice = prevCandleEntity.getClose();
            final BigDecimal currOpenPrice = currCandleEntity.getOpen();
            final BigDecimal currClosePrice = currCandleEntity.getClose();
            return TradeUtils.isGreaterThan(currOpenPrice, prevOpenPrice) && TradeUtils.isGreaterThan(currOpenPrice, prevClosePrice)
                    && TradeUtils.isLessThan(currClosePrice, prevOpenPrice) && TradeUtils.isGreaterThan(currClosePrice, prevClosePrice);
        }
        return false;
    }

    public static Boolean isBearishHaramiIndicator(List<CandleEntity> candles) {
        if (CollectionUtils.isEmpty(candles) || candles.size() < 2) {
            // Engulfing is a 2-candle pattern
            return false;
        }
        int index = candles.size() - 1;
        CandleEntity prevCandleEntity = candles.get(index - 1);
        CandleEntity currCandleEntity = candles.get(index);
        if (prevCandleEntity.isBullish() && currCandleEntity.isBearish()) {
            final BigDecimal prevOpenPrice = prevCandleEntity.getOpen();
            final BigDecimal prevClosePrice = prevCandleEntity.getClose();
            final BigDecimal currOpenPrice = currCandleEntity.getOpen();
            final BigDecimal currClosePrice = currCandleEntity.getClose();
            return TradeUtils.isGreaterThan(currOpenPrice, prevOpenPrice) && TradeUtils.isGreaterThan(currOpenPrice, prevClosePrice)
                    && TradeUtils.isGreaterThan(currClosePrice, prevOpenPrice) && TradeUtils.isLessThan(currClosePrice, prevClosePrice);
        }
        return false;
    }

    public static Boolean isBullishEngulfingIndicator(List<CandleEntity> candles) {
        if (CollectionUtils.isEmpty(candles) || candles.size() < 2) {
            // Engulfing is a 2-candle pattern
            return false;
        }
        int index = candles.size() - 1;
        CandleEntity prevCandleEntity = candles.get(index - 1);
        CandleEntity currCandleEntity = candles.get(index);
        if (prevCandleEntity.isBearish() && currCandleEntity.isBullish()) {
            final BigDecimal prevOpenPrice = prevCandleEntity.getOpen();
            final BigDecimal prevClosePrice = prevCandleEntity.getClose();
            final BigDecimal currOpenPrice = currCandleEntity.getOpen();
            final BigDecimal currClosePrice = currCandleEntity.getClose();
            return TradeUtils.isGreaterThan(currOpenPrice, prevOpenPrice) && TradeUtils.isGreaterThan(currOpenPrice, prevClosePrice)
                    && TradeUtils.isGreaterThan(currClosePrice, prevOpenPrice) && TradeUtils.isGreaterThan(currClosePrice, prevClosePrice);
        }
        return false;
    }

}
