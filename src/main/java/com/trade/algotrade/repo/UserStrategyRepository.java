package com.trade.algotrade.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.UserStrategyEntity;

@Repository
public interface UserStrategyRepository extends MongoRepository<UserStrategyEntity, String> {

	List<UserStrategyEntity> findByUserId(String userId);

	Optional<UserStrategyEntity> findByUserIdAndStrategyName(String userId, String strategyName);

	List<UserStrategyEntity> findByStrategyName(String strategyName);
}
