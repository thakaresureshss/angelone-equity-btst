package com.trade.algotrade.enitiy;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document("trade_orders")
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TradeOrderEntity {

    @Id
    private String id;
    private String tradeId;
    private Long orderId;
}
