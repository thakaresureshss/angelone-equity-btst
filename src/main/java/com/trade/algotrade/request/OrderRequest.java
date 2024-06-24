package com.trade.algotrade.request;

import com.trade.algotrade.client.angelone.enums.ProductType;
import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.client.kotak.request.TargetDetails;
import com.trade.algotrade.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private String userId;
    private TransactionType transactionType;
    private Integer quantity;
    private Integer filledQuantity;
    private BigDecimal price;
    private ProductType product;
//    private ValidityEnum validity;
    private VarietyEnum variety;
    private String tag;
    private SlDetails slDetails;
    private TargetDetails targetDetails;
    private String strategyName;
    private String instrumentName;
    private Long instrumentToken;
    private Integer strikePrice;
    private OptionType optionType;
    private OrderType orderType;
    private String segment;
    private OrderCategoryEnum orderCategory;
    private Long originalOrderId;
    private TriggerType triggerType=TriggerType.AUTO;
    private Long orderId;

    private boolean isMockOrder;
}
