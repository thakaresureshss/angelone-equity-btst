package com.trade.algotrade.enitiy;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("open_orders")
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenOrderEntity extends OrderEntity {
    @Id
    private String id;

}
