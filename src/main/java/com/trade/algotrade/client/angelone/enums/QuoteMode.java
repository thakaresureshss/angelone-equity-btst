package com.trade.algotrade.client.angelone.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QuoteMode {

    FULL("FULL"), OHLC("OHLC"), LTP("LTP");

    private final String value;

    QuoteMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String strValue() {
        return this.value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static QuoteMode fromValue(String value) {
        for (QuoteMode b : QuoteMode.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
