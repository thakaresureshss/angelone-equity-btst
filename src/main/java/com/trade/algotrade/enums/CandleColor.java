package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CandleColor {

  GREEN("GREEN"),

  RED("RED");

  private final String value;

  CandleColor(String value) {
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
  public static CandleColor fromValue(String value) {
    for (CandleColor b : CandleColor.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
