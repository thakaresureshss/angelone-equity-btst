package com.trade.algotrade.enitiy.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("timezone_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimezoneConfigEntity {
	private String timezone;
	private String timezoneKey;
	private String timezoneValue;
}
