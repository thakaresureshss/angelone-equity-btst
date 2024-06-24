package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Strategy {

    BIGCANDLE("BIGCANDLE"), EXPIRY("EXPIRY"), PULLBACK("PULLBACK"), EQUITY_TOP_MOVERS("EQUITY_TOP_MOVERS"),
    STRATEGY_EXPIRY_SCALPING("STRATEGY_EXPIRY_SCALPING"), STRATEGY_SCALPING("STRATEGY_SCALPING"), EQUITY_NEWS_SPIKE("EQUITY_NEWS_SPIKE"), BIGMOVE("BIGMOVE"), MANUAL("MANUAL");

    private final String value;

    Strategy(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static Strategy fromValue(String value) {
        for (Strategy b : Strategy.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
