package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BrokerEnum {

    ANGEL_ONE("ANGELONE"), KOTAK_SECURITIES("KOTAK_SECURITIES"), ALL("ALL");

    private final String value;

    BrokerEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static BrokerEnum fromValue(String value) {
        for (BrokerEnum b : BrokerEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
