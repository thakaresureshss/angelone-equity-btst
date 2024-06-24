package com.trade.algotrade.request;

import com.trade.algotrade.enums.OptionType;
import com.trade.algotrade.enums.Strategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StrategyOrderRequest {
    private Strategy strategy;
    private OptionType optionType;
}
