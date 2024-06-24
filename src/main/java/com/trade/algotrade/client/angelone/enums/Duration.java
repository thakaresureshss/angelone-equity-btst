package com.trade.algotrade.client.angelone.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Duration {

    DAY("DAY"), IOC("IOC");

    private final String value;

    Duration(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static Duration fromValue(String value) {
        for (Duration b : Duration.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
