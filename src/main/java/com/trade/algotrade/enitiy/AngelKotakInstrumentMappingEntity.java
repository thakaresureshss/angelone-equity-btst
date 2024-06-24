package com.trade.algotrade.enitiy;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Document("angel_kotak_instrument_mapping")
@ToString
public class AngelKotakInstrumentMappingEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long kotakInstrument;

    private Long angelInstrument;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}