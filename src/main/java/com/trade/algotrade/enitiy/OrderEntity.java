package com.trade.algotrade.enitiy;

import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.client.kotak.request.TargetDetails;
import com.trade.algotrade.enums.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    private String userId;
    private TransactionType transactionType;
    private Integer quantity;
    private BigDecimal price;
    private ValidityEnum validity;
    private VarietyEnum variety;
    private Boolean isSL;
    private Long instrumentToken;
    private Long orderId;
    private OptionType optionType;
    private Integer strikePrice;
    private OrderStatus orderStatus;
    private OrderType orderType;
    private String strategyName;
    private SlDetails slDetails;
    private TargetDetails targetDetails;
    private String segment;
    private Boolean isSlOrderPlaced;
    private OrderCategoryEnum orderCategory;
    private Long originalOrderId;

    private int modificationCount;
    private String tradeId;
    private TriggerType triggerType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SlDetails> SlDetailsHistory;
    private String squareOffReason;
}
