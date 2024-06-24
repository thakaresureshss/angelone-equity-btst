package com.trade.algotrade.enitiy;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("order_history")
@ToString
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClosedOrderEntity extends OrderEntity {

    @Id
    private String id;

}
