package com.trade.algotrade.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.exceptions.AlgotradeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.trade.algotrade.enitiy.ConfigurationEntity;
import com.trade.algotrade.repo.AlgoConfigurationRepository;
import com.trade.algotrade.request.ConfigurationRequest;
import com.trade.algotrade.response.ConfigurationResponse;
import com.trade.algotrade.service.AlgoConfigurationService;
import org.springframework.util.CollectionUtils;


/**
 * @author Rahul Pansare
 */
@Service
public class AlgoConfigurationServiceImpl implements AlgoConfigurationService {

    @Autowired
    private AlgoConfigurationRepository algoConfigurationRepository;

    @Override
    @CacheEvict(cacheNames = "configs", allEntries = true)
    public ConfigurationResponse createConfiguration(ConfigurationRequest configurationRequest) {
        ConfigurationEntity configurationEntity = new ConfigurationEntity();
        configurationEntity.setConfigurationDetails(configurationRequest.getConfigurationDetails());
        configurationEntity.setCreatedTime(LocalDateTime.now());
        configurationEntity.setUpdatedTime(LocalDateTime.now());

        ConfigurationEntity savedConfiguration = algoConfigurationRepository.save(configurationEntity);
        return mapEntityToResponse(savedConfiguration);
    }

    @Override
    @Cacheable(value = "configs")
    public ConfigurationResponse getConfigurationDetails() {
        List<ConfigurationEntity> allConfigrations = algoConfigurationRepository.findAll();
        if (CollectionUtils.isEmpty(allConfigrations)) {
            throw new AlgotradeException(ErrorCodeConstants.EMPTY_CONFIG_ERROR);
        }
        ConfigurationEntity configurationEntity = allConfigrations.stream().findFirst().get();
        return mapEntityToResponse(configurationEntity);
    }

    private ConfigurationResponse mapEntityToResponse(ConfigurationEntity configurationEntity) {
        ConfigurationResponse configurationResponse = new ConfigurationResponse();
        configurationResponse.setId(configurationEntity.getId());
        configurationResponse.setConfigurationDetails(configurationEntity.getConfigurationDetails());
        return configurationResponse;
    }

    @Override
    @CachePut(value = "strategies")
    public ConfigurationResponse modifyConfiguration(ConfigurationRequest configurationRequest) {
        ConfigurationEntity configurationEntity = algoConfigurationRepository.findAll().get(0);
        configurationEntity.setConfigurationDetails(configurationRequest.getConfigurationDetails());
        ConfigurationEntity savedConfiguration = algoConfigurationRepository.save(configurationEntity);
        return mapEntityToResponse(savedConfiguration);
    }


}
