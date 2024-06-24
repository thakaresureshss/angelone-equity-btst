package com.trade.algotrade.request;

import com.trade.algotrade.dto.TradeOrderConfigDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfigUpdateRequest {
    private String strategyName;
    private List<TradeOrderConfigDto> tradeQuantities;

}