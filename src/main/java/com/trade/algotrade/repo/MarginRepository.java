package com.trade.algotrade.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.MarginEntity;

@Repository
public interface MarginRepository extends MongoRepository<MarginEntity, String> {

	Optional<MarginEntity> findByUserId(String userId);

}