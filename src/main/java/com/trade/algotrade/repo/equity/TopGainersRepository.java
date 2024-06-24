package com.trade.algotrade.repo.equity;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.equity.TopGainers;

@Repository
public interface TopGainersRepository extends MongoRepository<TopGainers, Long> {

}
