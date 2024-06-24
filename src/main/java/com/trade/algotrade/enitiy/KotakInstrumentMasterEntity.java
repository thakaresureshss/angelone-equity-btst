package com.trade.algotrade.enitiy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@Document("kotak_instrument_master")
@ToString
public class KotakInstrumentMasterEntity {

	@Id
	private String id;

	@Indexed(unique = true)
	private Long instrumentToken;
	private String instrumentName;
	private String name;
	private BigDecimal lastPrice;
	private String expiry;
	private Integer strike;
	private BigDecimal tickSize;
	private Integer lotSize;
	private String instrumentType;
	private String segment;
	private String exchange;
	private String isin;
	private String multiplier;
	private String exchangeToken;
	private String optionType;
	private LocalDateTime createdTime;
	private LocalDateTime updatedTime;
}