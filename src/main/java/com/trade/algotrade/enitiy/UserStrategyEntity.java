package com.trade.algotrade.enitiy;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("user_strategy_master")
@ToString
public class UserStrategyEntity {

    @Id
    private Long id;
    private String userId;
    private String strategyName;
    private Integer quantity;
    private BigDecimal overallLoss;
    private BigDecimal overallProfit = BigDecimal.ZERO;
    private BigDecimal initialCapitalDeployed;
    private BigDecimal dayProfitPercent = BigDecimal.ZERO;
    private Integer bufferQuantityPercentage;
    private Integer failureCount;
    private Integer lastOrderQuantity;
}