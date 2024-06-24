package com.trade.algotrade.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.OrderStatasticsEntity;

@Repository
public interface OrderStatasticsRepository extends MongoRepository<OrderStatasticsEntity, Long>{

	
}
