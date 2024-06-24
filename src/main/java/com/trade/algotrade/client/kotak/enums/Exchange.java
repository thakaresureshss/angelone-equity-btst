package com.trade.algotrade.client.kotak.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Exchange {

  NSE("NSE"),

  BSE("BSE"),

  NFO("NFO");

  private final String value;

  Exchange(String value) {
    this.value = value;
  }

  @JsonValue
  public String toString() {
    return this.value;
  }

  @JsonCreator
  public static Exchange fromValue(String value) {
    for (Exchange b : Exchange.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

}
