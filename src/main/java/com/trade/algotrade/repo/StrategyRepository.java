package com.trade.algotrade.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.StrategyEnity;

@Repository
public interface StrategyRepository extends MongoRepository<StrategyEnity, String> {

	Optional<StrategyEnity> findByStrategyName(String strategyName);

}
