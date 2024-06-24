package com.trade.algotrade.client.angelone.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExchangeType {

    NSE("NSE"), BSE("BSE"), NFO("NFO");

    private final String value;

    ExchangeType(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static ExchangeType fromValue(String value) {
        for (ExchangeType b : ExchangeType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
