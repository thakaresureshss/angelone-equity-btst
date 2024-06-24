package com.trade.algotrade.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.trade.algotrade.enitiy.ConfigurationEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface AlgoConfigurationRepository extends MongoRepository<ConfigurationEntity, String>{

}
