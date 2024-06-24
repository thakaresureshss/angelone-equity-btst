package com.trade.algotrade.enitiy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document("trade_history")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistoryEntity extends BaseTradeEntity {

    @Id
    private String id;

    @DBRef
    private List<OpenOrderEntity> orders;
}
