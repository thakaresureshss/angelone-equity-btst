package com.trade.algotrade.enitiy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document("margin_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "margins_userid_idx", def = "{'userId' : 1}", unique = true)
public class MarginEntity {

	
	@Id
	private String id;
	private String userId;
	private Map<String, BigDecimal> margins;
	private LocalDateTime createdTime;
	private LocalDateTime modifiedTime;
}
