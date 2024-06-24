package com.trade.algotrade.chartpatterns;

import com.trade.algotrade.enitiy.CandleEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public class Pattern {
    public static boolean isCandleBullish(CandleEntity candle) {
        return candle.getClose().compareTo(candle.getOpen()) > 0;
    }

    public static boolean closedLastCandleAt50PercentOrMoreOfBodyFromCurrent(List<CandleEntity> candles) {
        CandleEntity current = candles.get(0);
        CandleEntity last = candles.get(2);
        BigDecimal lastBodyTop = getCandleBodyTop(last);
        BigDecimal curBodyTop = getCandleBodyTop(current);
        BigDecimal currentBodyTop50Percent = curBodyTop.subtract((getCandleBody(current).divide(BigDecimal.valueOf(2)).setScale(0, RoundingMode.DOWN)));
        return lastBodyTop.compareTo(currentBodyTop50Percent) >= 0;
//        return lastBodyTop >= currentBodyTop50Percent;
    }

    public static boolean tinyClosedAboveCurrentAndLast(List<CandleEntity> candles) {
        BigDecimal curBodyTop = getCandleBodyTop(candles.get(0));
        BigDecimal middleBodyBottom = getCandleBodyBottom(candles.get(1));
        BigDecimal lastBodyTop = getCandleBodyTop(candles.get(2));
        return middleBodyBottom.compareTo(curBodyTop) > 0 && middleBodyBottom.compareTo(lastBodyTop) > 0 && curBodyTop.compareTo(lastBodyTop) > 0;
        //return middleBodyBottom > curBodyTop && middleBodyBottom > lastBodyTop && curBodyTop > lastBodyTop;
    }

    public static boolean tinyClosedBelowCurrentAndLast(List<CandleEntity> candles) {
        BigDecimal curBodyBottom = getCandleBodyBottom(candles.get(0));
        BigDecimal middleBodyTop = getCandleBodyTop(candles.get(1));
        BigDecimal lastBodyBottom = getCandleBodyBottom(candles.get(2));
        //return middleBodyTop < curBodyBottom && middleBodyTop < lastBodyBottom && lastBodyBottom > curBodyBottom;
        return middleBodyTop.compareTo(curBodyBottom) < 0 && middleBodyTop.compareTo(lastBodyBottom) < 0 && lastBodyBottom.compareTo(curBodyBottom) > 0;
    }

    public static boolean currentBodySmallerThanLast(List<CandleEntity> candles) {
        BigDecimal curBody = getCandleBody(candles.get(0));
        BigDecimal lastBody = getCandleBody(candles.get(2));
        return curBody.compareTo(lastBody) < 0;
    }

    public static boolean doBothCandlesShareSameOrAlmostSameBody(List<CandleEntity> candles) {
        BigDecimal curBody = getCandleBody(candles.get(0));
        BigDecimal prevBody = getCandleBody(candles.get(0));
        BigDecimal percentage = BigDecimal.valueOf(0.5);
        return curBody.compareTo(prevBody) == 0 || valuePlusMinus(percentage, curBody, prevBody);
    }

    public static boolean closedCurrentCandleAt50PercentOrBellowOfBodyFromPrevious(List<CandleEntity> candles) {
        CandleEntity current = candles.get(0);
        CandleEntity previous = candles.get(1);
        BigDecimal curBodyBottom = getCandleBodyBottom(current);
        BigDecimal prevBodyBottom = getCandleBodyBottom(previous);
        BigDecimal prevBodyBottom50Percent = prevBodyBottom.add((getCandleBody(previous).divide(BigDecimal.valueOf(2)).setScale(0, RoundingMode.DOWN)));
        return curBodyBottom.compareTo(prevBodyBottom50Percent) < 0 || curBodyBottom.compareTo(prevBodyBottom50Percent) == 0;
    }

    public static boolean eachCandleClosedAtSuccessivelyHigherShadow(List<CandleEntity> candles) {
        BigDecimal lastUpperShadow = candles.get(2).getHigh();
        BigDecimal midBottom = getCandleBodyBottom(candles.get(1));
        BigDecimal midLowerShadow = candles.get(1).getLow();
        BigDecimal midUpperShadow = candles.get(1).getHigh();
        BigDecimal curBottom = getCandleBodyBottom(candles.get(0));
        BigDecimal curLowerShadow = candles.get(0).getLow();
        return lastUpperShadow.compareTo(midBottom) <= 0 && lastUpperShadow.compareTo(midLowerShadow) >= 0 && midUpperShadow.compareTo(curBottom) <= 0 && midUpperShadow.compareTo(curLowerShadow) >= 0;
    }

    public static boolean eachCandleClosedAtSuccessivelyLowerShadow(List<CandleEntity> candles) {
        BigDecimal curUpperShadow = candles.get(0).getHigh();
        BigDecimal midBottom = getCandleBodyBottom(candles.get(1));
        BigDecimal midLowerShadow = candles.get(1).getLow();
        BigDecimal midUpperShadow = candles.get(1).getHigh();
        BigDecimal lastBottom = getCandleBodyBottom(candles.get(2));
        BigDecimal lastLowerShadow = candles.get(2).getLow();
        //return curUpperShadow <= midBottom && curUpperShadow >= midLowerShadow && midUpperShadow <= lastBottom && midUpperShadow >= lastLowerShadow;
        return curUpperShadow.compareTo(midBottom) <= 0 && curUpperShadow.compareTo(midLowerShadow) >= 0 && midUpperShadow.compareTo(lastBottom) <= 0 && midUpperShadow.compareTo(lastLowerShadow) >= 0;
    }


    public static boolean closedCurrentCandleAt50PercentOrMoreOfBodyFromLast(List<CandleEntity> candles) {
        CandleEntity current = candles.get(0);
        CandleEntity last = candles.get(2);
        BigDecimal curBodyTop = getCandleBodyTop(current);
        BigDecimal lastBodyBottom = getCandleBodyBottom(last);
        BigDecimal lastBodyBottom50Percent = lastBodyBottom.add((getCandleBody(last).divide(BigDecimal.valueOf(2)).setScale(0, RoundingMode.DOWN)));
        return curBodyTop.compareTo(lastBodyBottom50Percent) >= 0;
    }

    public static boolean formedTinyMiddleCandle(List<CandleEntity> candles) {
        BigDecimal curBody = getCandleBody(candles.get(0));
        BigDecimal prevBody = getCandleBody(candles.get(1));
        BigDecimal lastBody = getCandleBody(candles.get(2));
        return prevBody.compareTo(curBody) < 0 && prevBody.compareTo(lastBody) < 0;
        //return prevBody < curBody && prevBody < lastBody;
    }

    public static boolean openedCurrentCandleBellowOrAtClosingFromPrevious(List<CandleEntity> candles) {
        CandleEntity current = candles.get(0);
        CandleEntity previous = candles.get(1);
        BigDecimal curBodyTop = getCandleBodyTop(current);
        BigDecimal prevBodyTop = getCandleBodyTop(previous);
        //return curBodyTop < prevBodyTop || curBodyTop == prevBodyTop;
        return curBodyTop.compareTo(prevBodyTop) < 0 || curBodyTop.compareTo(prevBodyTop) == 0;
    }

    public static boolean doBothCandlesShareSameOrAlmostSameHigh(List<CandleEntity> candles) {
        BigDecimal curShadowUpper = getCandleUpperShadow(candles.get(0));
        BigDecimal prevShadowUpper = getCandleUpperShadow(candles.get(0));
        BigDecimal percentage = BigDecimal.valueOf(0.5);
        return curShadowUpper.compareTo(prevShadowUpper) == 0 || valuePlusMinus(percentage, curShadowUpper, prevShadowUpper);
    }

    public static boolean eachCandleClosedSuccessivelyLower(List<CandleEntity> candles) {
        BigDecimal curTop = getCandleBodyTop(candles.get(0));
        BigDecimal midTop = getCandleBodyTop(candles.get(1));
        BigDecimal lastTop = getCandleBodyTop(candles.get(2));
//        return lastTop > midTop && midTop > curTop;
        return lastTop.compareTo(midTop) > 0 && midTop.compareTo(curTop) > 0;
    }

    public static boolean eachCandleClosedSuccessivelyHigher(List<CandleEntity> candles) {
        BigDecimal curTop = getCandleBodyTop(candles.get(0));
        BigDecimal midTop = getCandleBodyTop(candles.get(1));
        BigDecimal lastTop = getCandleBodyTop(candles.get(2));
        //return lastTop < midTop && midTop < curTop;
        return lastTop.compareTo(midTop) < 0 && midTop.compareTo(curTop) < 0;
    }

    public static boolean formedThreeConsecutiveLongCandles(List<CandleEntity> candles) {
        BigDecimal coef1 = Pattern.getCandleBody(candles.get(0)).divide(Pattern.getCandleUpperShadow(candles.get(0))).setScale(0, RoundingMode.DOWN);
        BigDecimal coef2 = Pattern.getCandleBody(candles.get(0)).divide(Pattern.getCandleLowerShadow(candles.get(0))).setScale(0, RoundingMode.DOWN);
        BigDecimal coef3 = Pattern.getCandleBody(candles.get(1)).divide(Pattern.getCandleUpperShadow(candles.get(1))).setScale(0, RoundingMode.DOWN);
        BigDecimal coef4 = Pattern.getCandleBody(candles.get(1)).divide(Pattern.getCandleLowerShadow(candles.get(1))).setScale(0, RoundingMode.DOWN);
        BigDecimal coef5 = Pattern.getCandleBody(candles.get(2)).divide(Pattern.getCandleUpperShadow(candles.get(2))).setScale(0, RoundingMode.DOWN);
        BigDecimal coef6 = Pattern.getCandleBody(candles.get(2)).divide(Pattern.getCandleLowerShadow(candles.get(2))).setScale(0, RoundingMode.DOWN);
        //return coef1 >= 5 && coef2 >= 5 && coef3 >= 5 && coef4 >= 5 && coef5 >= 5 && coef6 >= 5;
        return coef1.compareTo(BigDecimal.valueOf(5)) >= 0 && coef2.compareTo(BigDecimal.valueOf(5)) >= 0 && coef3.compareTo(BigDecimal.valueOf(5)) >= 0 && coef4.compareTo(BigDecimal.valueOf(5)) >= 0 && coef5.compareTo(BigDecimal.valueOf(5)) >= 0 && coef6.compareTo(BigDecimal.valueOf(5)) >= 0;
    }

    public static boolean closedCurrentCandleAt50PercentOrAboveOfBodyFromPrevious(List<CandleEntity> candles) {
        CandleEntity current = candles.get(0);
        CandleEntity previous = candles.get(1);
        BigDecimal curBodyTop = getCandleBodyTop(current);
        BigDecimal prevBodyBottom = getCandleBodyBottom(previous);
        BigDecimal prevBodyBottom50Percent = prevBodyBottom.add((getCandleBody(previous).divide(BigDecimal.valueOf(2)).setScale(0, RoundingMode.DOWN)));
        return curBodyTop.compareTo(prevBodyBottom50Percent) > 0 || curBodyTop.compareTo(prevBodyBottom50Percent) == 0;
    }

    public static boolean openedCurrentCandleAboveOrAtClosingFromPrevious(List<CandleEntity> candles) {
        CandleEntity current = candles.get(0);
        CandleEntity previous = candles.get(1);
        BigDecimal curBodyTop = getCandleBodyTop(current);
        BigDecimal prevBodyTop = getCandleBodyTop(previous);
        //return curBodyTop > prevBodyTop || curBodyTop == prevBodyTop;
        return curBodyTop.compareTo(prevBodyTop) > 0 || curBodyTop.compareTo(prevBodyTop) == 0;
    }

    public static boolean doBothCandlesShareSameOrAlmostSameLow(List<CandleEntity> candles) {
        BigDecimal curShadowLow = getCandleLowerShadow(candles.get(0));
        BigDecimal prevShadowLow = getCandleLowerShadow(candles.get(0));
        BigDecimal percentage = BigDecimal.valueOf(0.5);
        return curShadowLow.compareTo(prevShadowLow) == 0 || valuePlusMinus(percentage, curShadowLow, prevShadowLow);
    }

    public static boolean valuePlusMinus(BigDecimal percentage, BigDecimal current, BigDecimal previous) {
        BigDecimal drift = ((current.multiply(percentage)).divide(BigDecimal.valueOf(100))).setScale(0, RoundingMode.DOWN);
        BigDecimal high = current.add(drift);
        BigDecimal low = current.subtract(drift);
        //return previous <= high && previous >= low;
        return previous.compareTo(high) <= 0 && previous.compareTo(low) >= 0;
    }

    public static boolean isPreviousCandleEntirelyContainedInBodyOfCurrent(List<CandleEntity> candles) {
        CandleEntity current = candles.get(0);
        CandleEntity previous = candles.get(1);
        BigDecimal curBodyTop = getCandleBodyTop(current);
        BigDecimal curBodyBottom = getCandleBodyBottom(current);
        BigDecimal prevBodyTop = getCandleBodyTop(previous);
        BigDecimal prevBodyBottom = getCandleBodyBottom(previous);
        //return curBodyTop > prevBodyTop && curBodyBottom < prevBodyBottom;
        return curBodyTop.compareTo(prevBodyTop) > 0 && curBodyBottom.compareTo(prevBodyBottom) < 0;
    }

    public static boolean isPreviousCandleShorterThanCurrent(List<CandleEntity> candles) {
        CandleEntity current = candles.get(0);
        CandleEntity previous = candles.get(1);
        BigDecimal curBody = getCandleBody(current);
        BigDecimal prevBody = getCandleBody(previous);
//        return prevBody < curBody;
        return prevBody.compareTo(curBody) < 0;
    }

    public static boolean hasLittleOrNoLowerShadow(CandleEntity candle) {
        BigDecimal coefficient = Pattern.getCandleLowerShadow(candle).divide(Pattern.getCandleBody(candle));
        // return coefficient <= 2; // lower shadow <= 2x body size
        return coefficient.compareTo(BigDecimal.valueOf(2)) <= 0; // lower shadow <= 2x body size
    }

    public static boolean hasLittleOrNoUpperShadow(CandleEntity candle) {
        BigDecimal coefficient = Pattern.getCandleUpperShadow(candle).divide(Pattern.getCandleBody(candle)).setScale(0, RoundingMode.DOWN);
        //return coefficient <= 2; // upper shadow <= 2x body size
        return coefficient.compareTo(BigDecimal.valueOf(2)) <= 0; // lower shadow <= 2x body size
    }

    public static boolean isCandleBearish(CandleEntity candle) {
        return candle.getClose().compareTo(candle.getOpen()) < 0;
    }

    public static boolean isCandleNeitherBullishNorBearish(CandleEntity candle) {
        return candle.getClose().compareTo(candle.getOpen()) == 0;
    }

    public static boolean hasLongUpperShadow(CandleEntity candle) {
        BigDecimal coefficient = Pattern.getCandleUpperShadow(candle).divide(Pattern.getCandleBody(candle)).setScale(0, RoundingMode.DOWN);
        return coefficient.compareTo(BigDecimal.valueOf(5)) >= 0; // upper shadow >= 5x body size
    }

    public static boolean hasLongLowerShadow(CandleEntity candle) {
        BigDecimal coefficient = Pattern.getCandleLowerShadow(candle).divide(Pattern.getCandleBody(candle)).setScale(0, RoundingMode.DOWN);
        return coefficient.compareTo(BigDecimal.valueOf(5)) >= 0; // lower shadow >= 5x body size
    }

    public static boolean isUpperShadow2xLongerThanBody(CandleEntity candle) {
        BigDecimal coefficient = Pattern.getCandleUpperShadow(candle).divide(Pattern.getCandleBody(candle)).setScale(0, RoundingMode.DOWN);
        return coefficient.compareTo(BigDecimal.valueOf(2)) >= 0; // upper shadow >= 2x body size
    }

    public static boolean isLowerShadow2xLongerThanBody(CandleEntity candle) {
        BigDecimal coefficient = Pattern.getCandleLowerShadow(candle).divide(Pattern.getCandleBody(candle)).setScale(0, RoundingMode.DOWN);
        // lower shadow >= 2x body size
        return coefficient.compareTo(BigDecimal.valueOf(2)) >= 0;
    }

    public static boolean hasLittleOrNoRealBody(CandleEntity candle) {
        boolean result;
        if (Pattern.isCandleNeitherBullishNorBearish(candle)) {
            result = true;
        } else {
            BigDecimal coefficient = Pattern.getCandleFullSize(candle).divide(Pattern.getCandleBody(candle)).setScale(0, RoundingMode.DOWN);
            result = coefficient.compareTo(BigDecimal.valueOf(7)) >= 0;
        }
        return result;
    }

    public static BigDecimal getCandleBody(CandleEntity candle) {
        BigDecimal candleBody = candle.getClose().subtract(candle.getOpen());
        if (candleBody.compareTo(BigDecimal.ZERO) < 0) {
            candleBody = candleBody.multiply(BigDecimal.valueOf((-1)));
        }
        return candleBody;
    }

    public static BigDecimal getCandleBodyTop(CandleEntity candle) {
        BigDecimal candleBodyTop;
        if (Pattern.isCandleBullish(candle)) {
            candleBodyTop = candle.getClose();
        } else if (Pattern.isCandleBearish(candle)) {
            candleBodyTop = candle.getOpen();
        } else {
            // neither bullish nor bearish
            candleBodyTop = candle.getClose();
        }
        return candleBodyTop;
    }

    public static BigDecimal getCandleBodyBottom(CandleEntity candle) {
        BigDecimal candleBodyBottom;
        if (Pattern.isCandleBullish(candle)) {
            candleBodyBottom = candle.getOpen();
        } else if (Pattern.isCandleBearish(candle)) {
            candleBodyBottom = candle.getClose();
        } else {
            // neither bullish nor bearish
            candleBodyBottom = candle.getClose();
        }
        return candleBodyBottom;
    }

    public static BigDecimal getCandleUpperShadow(CandleEntity candle) {
        BigDecimal candleUpperShadow;
        if (Pattern.isCandleBullish(candle)) {
            candleUpperShadow = candle.getHigh().subtract(candle.getClose());
        } else if (Pattern.isCandleBearish(candle)) {
            candleUpperShadow = candle.getHigh().subtract(candle.getOpen());
        } else {
            // neither bullish nor bearish
            candleUpperShadow = candle.getHigh().subtract(candle.getClose());
        }
        return candleUpperShadow;
    }

    public static BigDecimal getCandleLowerShadow(CandleEntity candle) {
        BigDecimal candleLowerShadow;
        if (Pattern.isCandleBullish(candle)) {
            candleLowerShadow = candle.getOpen().subtract(candle.getLow());
        } else if (Pattern.isCandleBearish(candle)) {
            candleLowerShadow = candle.getClose().subtract(candle.getLow());
        } else {
            // neither bullish nor bearish
            candleLowerShadow = candle.getOpen().subtract(candle.getLow());
        }
        return candleLowerShadow;
    }

    public static BigDecimal getCandleFullSize(CandleEntity candle) {
        return candle.getHigh().subtract(candle.getLow());
    }

    public static boolean isCandleListValid(List<CandleEntity> candles) {
        if (candles.isEmpty()) {
            return false;
        } else {
            for (CandleEntity candle : candles) {
                if (Objects.isNull(candle)) {
                    return false;
                }
            }
        }
        return true;
    }
}