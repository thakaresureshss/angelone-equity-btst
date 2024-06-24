package com.trade.algotrade.client.kotak.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatusWebsocket {

    /**
     * variety regular Regular order amo After Market Order co Cover Order ? iceberg
     * Iceberg Order ? *
     */

    FIL("FIL"), AMO("AMO"), OPF("OPF"), OPN("OPN"), SLO("SLO"),TRAD("TRAD");


    private final String value;

    OrderStatusWebsocket(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static OrderStatusWebsocket fromValue(String value) {
        for (OrderStatusWebsocket b : OrderStatusWebsocket.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
