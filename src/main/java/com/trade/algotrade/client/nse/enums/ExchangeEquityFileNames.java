package com.trade.algotrade.client.nse.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExchangeEquityFileNames {

  NIFTY("nifty"),
  NIFTY_LARGE_CAP("niftylargecap"),
  NIFTY_SMALL_CAP("niftysmallcap"),
  NIFTY_MID_CAP("niftymidcap");

  private final String value;

  ExchangeEquityFileNames(String value) {
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
  public static ExchangeEquityFileNames fromValue(String value) {
    for (ExchangeEquityFileNames b : ExchangeEquityFileNames.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
