package com.trade.algotrade.enitiy;

import java.time.LocalDateTime;

import com.trade.algotrade.client.kotak.enums.Exchange;
import com.trade.algotrade.enums.Segment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@Document("instrument_watch_master")
@ToString
public class InstrumentWatchEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long instrumentToken;

    private String instrumentName;

    Exchange exchange;

    Segment segment;

    private String name;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}