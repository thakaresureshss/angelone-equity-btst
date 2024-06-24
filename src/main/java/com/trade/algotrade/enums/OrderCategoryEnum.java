package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderCategoryEnum {

    NEW("NEW"), SQAREOFF("SQAREOFF");

    private final String value;

    OrderCategoryEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static OrderCategoryEnum fromValue(String value) {
        for (OrderCategoryEnum b : OrderCategoryEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
