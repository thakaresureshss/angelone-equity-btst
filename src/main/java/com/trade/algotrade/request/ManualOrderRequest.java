package com.trade.algotrade.request;

import com.trade.algotrade.enums.ManualStrategy;
import com.trade.algotrade.enums.OptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManualOrderRequest {

    private ManualStrategy strategyName;

    private OptionType optionType;

}
