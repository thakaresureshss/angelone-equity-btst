package com.trade.algotrade.request;

import com.trade.algotrade.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TraderFilterQuery {
    String userId;
    TradeStatus tradeStatus;
    String fromDate;
    String toDate;
    Segment segment;
    TradeState tradeState;

}
