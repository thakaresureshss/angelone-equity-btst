package com.trade.algotrade.response;

import com.trade.algotrade.dto.ConfigurationDetails;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationResponse {
	
	private String id;
	
	private ConfigurationDetails configurationDetails;

}
