package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TriggerType {

    AUTO("AUTO"), MANUAL("MANUAL");

    private final String value;

    TriggerType(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static TriggerType fromValue(String value) {
        for (TriggerType b : TriggerType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
