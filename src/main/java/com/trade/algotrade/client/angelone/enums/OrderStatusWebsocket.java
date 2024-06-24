package com.trade.algotrade.client.angelone.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatusWebsocket {

    /**
     * variety regular Regular order amo After Market Order co Cover Order ? iceberg
     * Iceberg Order ? *
     */

    FIL("complete"), AMO("AMO"), OPF("OPF"), OPN("open"), SLO("SLO"),TRAD("TRAD");


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
