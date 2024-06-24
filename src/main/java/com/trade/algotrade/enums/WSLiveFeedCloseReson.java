package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WSLiveFeedCloseReson {

    EMPTY_INSTRUMENT("NO INSTRUMENT TO LISTEN");

    private final String value;

    WSLiveFeedCloseReson(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static WSLiveFeedCloseReson fromValue(String value) {
        for (WSLiveFeedCloseReson b : WSLiveFeedCloseReson.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
