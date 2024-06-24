package com.trade.algotrade.client.kotak.response;

import java.io.IOException;

public enum SegmentName {
  CLEARING, COMMODITY_EVENING, COMMODITY_MORNING, CURRENCY, EQUITY, F_O;
  public String toValue() {
    switch (this) {
      case CLEARING:
        return "Clearing";
      case COMMODITY_EVENING:
        return "Commodity Evening";
      case COMMODITY_MORNING:
        return "Commodity Morning";
      case CURRENCY:
        return "Currency";
      case EQUITY:
        return "Equity";
      case F_O:
        return "F & O";
    }
    return null;
  }

  public static SegmentName forValue(String value) throws IOException {
    if (value.equals("Clearing"))
      return CLEARING;
    if (value.equals("Commodity Evening"))
      return COMMODITY_EVENING;
    if (value.equals("Commodity Morning"))
      return COMMODITY_MORNING;
    if (value.equals("Currency"))
      return CURRENCY;
    if (value.equals("Equity"))
      return EQUITY;
    if (value.equals("F & O"))
      return F_O;
    throw new IOException("Cannot deserialize SegmentName");
  }
}
