package com.trade.algotrade.chartpatterns;

import com.trade.algotrade.enitiy.CandleEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SingleCandleStickPattern implements CandleStick {
    public List<CandleEntity> candles = new ArrayList<>();

    public SingleCandleStickPattern(List<CandleEntity> candles) {
        if (Pattern.isCandleListValid(candles)) {
            this.candles = candles;
        }
    }

    public boolean isBullish() {
        return isBullishDoji() || isBullishHammer();
    }

    public boolean isBearish() {
        return isBearishDoji() || isBearishShootingStar();
    }

    public boolean isBullishDoji() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if ((Pattern.isCandleBullish(this.candles.get(0)) ||
                    Pattern.isCandleNeitherBullishNorBearish(this.candles.get(0))) &&
                    Pattern.hasLittleOrNoRealBody(this.candles.get(0)) &&
                    Pattern.hasLittleOrNoUpperShadow(this.candles.get(0)) &&
                    Pattern.hasLongLowerShadow(this.candles.get(0))) {
                result = true;
            }
        }
        return result;
    }

    public boolean isBullishHammer() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBullish(this.candles.get(0)) &&
                    Pattern.hasLittleOrNoUpperShadow(this.candles.get(0)) &&
                    Pattern.isLowerShadow2xLongerThanBody(this.candles.get(0))) {
                result = true;
            }
        }
        return result;
    }

    public boolean isBearishDoji() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if ((Pattern.isCandleBearish(this.candles.get(0)) ||
                    Pattern.isCandleNeitherBullishNorBearish(this.candles.get(0))) &&
                    Pattern.hasLittleOrNoRealBody(this.candles.get(0)) &&
                    Pattern.hasLittleOrNoLowerShadow(this.candles.get(0)) &&
                    Pattern.hasLongUpperShadow(this.candles.get(0))) {
                result = true;
            }
        }
        return result;
    }

    public boolean isBearishShootingStar() {
        boolean result = false;
        if (Pattern.isCandleListValid(this.candles)) {
            if (Pattern.isCandleBearish(this.candles.get(0)) &&
                    Pattern.hasLittleOrNoLowerShadow(this.candles.get(0)) &&
                    Pattern.isUpperShadow2xLongerThanBody(this.candles.get(0))) {
                result = true;
            }
        }
        return result;
    }
}
	
	