package com.trade.algotrade.chartpatterns;

import com.trade.algotrade.enitiy.CandleEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Use it to check for double candle stick pattern signals
 * <p>
 * candles the most recent last three ticks.
 *
 * @return DoubleCandleStickPattern on which you call isBullish() or isBearish() on it.
 */
@Component
public class DoubleCandleStickPattern implements CandleStick {
    private List<CandleEntity> candles = new ArrayList<>();

    private DoubleCandleStickPattern(List<CandleEntity> candles) {
        if (candles != null && !candles.isEmpty() && candles.size() > 1) {
            this.candles = candles;
        }
    }

    public boolean isBullish() {
        return isBullishEngolfer() || isBullishPiercing() || isBullishTweezerBottom();
    }

    public boolean isBearish() {
        return isBearishEngolfer() || isBearishDarkCloudCover() || isBearishTweezerTop();
    }

    public boolean isBullishEngolfer() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(0)) &&
                    Pattern.isCandleBearish(this.candles.get(1)) &&
                    Pattern.isPreviousCandleShorterThanCurrent(this.candles) &&
                    Pattern.isPreviousCandleEntirelyContainedInBodyOfCurrent(this.candles)) {
                result = true;
            }
        }
        return result;
    }

    public boolean isBearishEngolfer() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(1)) &&
                    Pattern.isCandleBearish(this.candles.get(0)) &&
                    Pattern.isPreviousCandleShorterThanCurrent(this.candles) &&
                    Pattern.isPreviousCandleEntirelyContainedInBodyOfCurrent(this.candles)) {
                result = true;
            }
        }
        return result;
    }

    private boolean isBullishTweezerBottom() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(0)) &&
                    Pattern.isCandleBearish(this.candles.get(1)) &&
                    Pattern.doBothCandlesShareSameOrAlmostSameBody(this.candles) &&
                    Pattern.doBothCandlesShareSameOrAlmostSameLow(this.candles)) {
                result = true;
            }
        }
        return result;
    }

    private boolean isBullishPiercing() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(0)) &&
                    Pattern.isCandleBearish(this.candles.get(1)) &&
                    Pattern.openedCurrentCandleBellowOrAtClosingFromPrevious(this.candles) &&
                    Pattern.closedCurrentCandleAt50PercentOrAboveOfBodyFromPrevious(this.candles)) {
                result = true;
            }
        }
        return result;
    }


    private boolean isBearishDarkCloudCover() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(1)) &&
                    Pattern.isCandleBearish(this.candles.get(0)) &&
                    Pattern.openedCurrentCandleAboveOrAtClosingFromPrevious(this.candles) &&
                    Pattern.closedCurrentCandleAt50PercentOrBellowOfBodyFromPrevious(this.candles)) {
                result = true;
            }
        }
        return result;
    }

    private boolean isBearishTweezerTop() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(1)) &&
                    Pattern.isCandleBearish(this.candles.get(0)) &&
                    Pattern.doBothCandlesShareSameOrAlmostSameBody(this.candles) &&
                    Pattern.doBothCandlesShareSameOrAlmostSameHigh(this.candles)) {
                result = true;
            }
        }
        return result;
    }
}