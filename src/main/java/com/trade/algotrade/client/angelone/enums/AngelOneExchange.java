package com.trade.algotrade.client.angelone.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AngelOneExchange {

    NSE("NSE"), NFO("NFO"), BSE("BSE"), BFO("BFO"), CDS("CDS"), MCX("MCX");

    private final String value;

    AngelOneExchange(String value) {
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
    public static AngelOneExchange fromValue(String value) {
        for (AngelOneExchange b : AngelOneExchange.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
