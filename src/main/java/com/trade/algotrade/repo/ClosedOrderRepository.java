package com.trade.algotrade.repo;

import com.trade.algotrade.repo.filter.FilterableRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.ClosedOrderEntity;

@Repository
public interface ClosedOrderRepository extends MongoRepository<ClosedOrderEntity, String>, FilterableRepository<ClosedOrderEntity> {

}
