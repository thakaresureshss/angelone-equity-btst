package com.trade.algotrade.client.angelone.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CandleTimeFrame {

    ONE_MINUTE("ONE_MINUTE"), FIVE_MINUTE("FIVE_MINUTE"), TEN_MINUTE("TEN_MINUTE"), FIFTEEN_MINUTE("FIFTEEN_MINUTE"), THIRTY_MINUTE("THIRTY_MINUTE"), ONE_HOUR("ONE_HOUR"), ONE_DAY("ONE_DAY");

    private final String value;

    CandleTimeFrame(String value) {
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
    public static CandleTimeFrame fromValue(String value) {
        for (CandleTimeFrame b : CandleTimeFrame.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
