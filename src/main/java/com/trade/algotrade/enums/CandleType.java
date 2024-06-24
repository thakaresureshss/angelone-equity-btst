package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CandleType {

  HAMMER("HAMMER"), GRAVESTONE("GRAVESTONE"), DRAGONFLY("DRAGONFLY"), BULLISH("BULLISH"),
  BEARISH("BEARISH"), SHOOTING_STAR("SHOOTING STAR"), DOJIS("DOJIS");

  private final String value;

  CandleType(String value) {
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
  public static CandleType fromValue(String value) {
    for (CandleType b : CandleType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
