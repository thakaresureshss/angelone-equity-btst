package com.trade.algotrade.service;

import com.trade.algotrade.request.ConfigurationRequest;
import com.trade.algotrade.response.ConfigurationResponse;

public interface AlgoConfigurationService {
	
	ConfigurationResponse createConfiguration(ConfigurationRequest configurationRequest);
	
	ConfigurationResponse getConfigurationDetails();
	
	ConfigurationResponse modifyConfiguration(ConfigurationRequest configurationRequest);
}
