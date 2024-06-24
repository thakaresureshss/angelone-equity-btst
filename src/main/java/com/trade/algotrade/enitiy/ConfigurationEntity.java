package com.trade.algotrade.enitiy;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.trade.algotrade.dto.ConfigurationDetails;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document("config_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationEntity {
	
	@Id
	private String id;
	
	private ConfigurationDetails configurationDetails;
	
	private LocalDateTime createdTime;
	private LocalDateTime updatedTime;
}
