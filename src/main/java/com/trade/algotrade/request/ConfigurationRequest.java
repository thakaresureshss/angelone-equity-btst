package com.trade.algotrade.request;

import java.math.BigDecimal;

import com.trade.algotrade.dto.ConfigurationDetails;
import com.trade.algotrade.dto.EntryCondition;
import com.trade.algotrade.dto.ExitCondition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationRequest {

	private String id;
	
	private ConfigurationDetails configurationDetails;
}
