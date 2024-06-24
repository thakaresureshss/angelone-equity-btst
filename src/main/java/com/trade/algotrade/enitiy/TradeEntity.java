package com.trade.algotrade.enitiy;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("open_trades")
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeEntity extends  BaseTradeEntity {

    @Id
    private String id;

    @DBRef
    private List<OpenOrderEntity> orders;
 }
