package com.trade.algotrade.chartpatterns;

import com.trade.algotrade.enitiy.CandleEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TripleCandleStickPattern implements CandleStick {
    private List<CandleEntity> candles = new ArrayList<>();

    private TripleCandleStickPattern(List<CandleEntity> candles) {
        if (candles != null && !candles.isEmpty() && candles.size() > 2) {
            this.candles = candles;
        }
    }

    public boolean isBearish() {
        return isBearishEveningStar() || isBearishThreeBlackCrows() || isBearishKicker();
    }

    public boolean isBullish() {
        return isBullishMorningStar() || isBullishWhiteSoldiers() || isBullishKicker();
    }

    public boolean isBullishMorningStar() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(0)) &&
                    Pattern.isCandleBullish(this.candles.get(1)) &&
                    Pattern.isCandleBearish(this.candles.get(2)) &&
                    Pattern.formedTinyMiddleCandle(this.candles) &&
                    Pattern.currentBodySmallerThanLast(this.candles) &&
                    Pattern.tinyClosedBelowCurrentAndLast(this.candles) &&
                    Pattern.closedCurrentCandleAt50PercentOrMoreOfBodyFromLast(this.candles)) {
                result = true;
            }
        }
        return result;
    }

    public boolean isBearishEveningStar() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBearish(this.candles.get(0)) &&
                    Pattern.isCandleBearish(this.candles.get(1)) &&
                    Pattern.isCandleBullish(this.candles.get(2)) &&
                    Pattern.formedTinyMiddleCandle(this.candles) &&
                    Pattern.currentBodySmallerThanLast(this.candles) &&
                    Pattern.tinyClosedAboveCurrentAndLast(this.candles) &&
                    Pattern.closedLastCandleAt50PercentOrMoreOfBodyFromCurrent(this.candles)) {
                result = true;
            }
        }
        return result;
    }



    public boolean isBullishWhiteSoldiers() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(0)) &&
                    Pattern.isCandleBullish(this.candles.get(1)) &&
                    Pattern.isCandleBullish(this.candles.get(2)) &&
                    Pattern.formedThreeConsecutiveLongCandles(this.candles) &&
                    Pattern.eachCandleClosedSuccessivelyHigher(this.candles)) {
                result = true;
            }
        }
        return result;
    }

    private boolean isBullishKicker() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBearish(this.candles.get(0)) &&
                    Pattern.isCandleBearish(this.candles.get(1)) &&
                    Pattern.isCandleBearish(this.candles.get(2)) &&
                    Pattern.eachCandleClosedAtSuccessivelyLowerShadow(this.candles)) {
                result = true;
            }
        }
        return result;
    }


    private boolean isBearishThreeBlackCrows() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBearish(this.candles.get(0)) &&
                    Pattern.isCandleBearish(this.candles.get(1)) &&
                    Pattern.isCandleBearish(this.candles.get(2)) &&
                    Pattern.formedThreeConsecutiveLongCandles(this.candles) &&
                    Pattern.eachCandleClosedSuccessivelyLower(this.candles)) {
                result = true;
            }
        }
        return result;
    }


    private boolean isBearishKicker() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(0)) &&
                    Pattern.isCandleBullish(this.candles.get(1)) &&
                    Pattern.isCandleBullish(this.candles.get(2)) &&
                    Pattern.eachCandleClosedAtSuccessivelyHigherShadow(this.candles)) {
                result = true;
            }
        }
        return result;
    }
}