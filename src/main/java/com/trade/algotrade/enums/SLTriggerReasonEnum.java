package com.trade.algotrade.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SLTriggerReasonEnum {

    INTRADAY_SQAREOFF("SL Intra-Day Square off scheduler triggered"), EXECUTE_SL("SL triggered"), REVERSAL_SL("SL triggered at market reverse"), DAY_LOSS_EXCEED("Current Trade exceeded day max loss limit"), TARGET_REACH("Target is reached"), SL_TIME_LIMIT_EXCEED("SL execution time limit exceed"), NO_MONITOR_SQUARE_OFF("Monitor flow not started");

    private final String value;

    SLTriggerReasonEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return this.value;
    }

    @JsonCreator
    public static SLTriggerReasonEnum fromValue(String value) {
        for (SLTriggerReasonEnum b : SLTriggerReasonEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
