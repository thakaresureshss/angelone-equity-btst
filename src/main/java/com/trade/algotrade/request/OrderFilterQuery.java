package com.trade.algotrade.request;

import com.trade.algotrade.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderFilterQuery {
    String userId;
    OrderStatus orderStatus;
    OrderType orderType;
    OrderCategoryEnum orderCategory;
    TransactionType transactionType;
    Segment segment;
    OrderState orderState;
}
