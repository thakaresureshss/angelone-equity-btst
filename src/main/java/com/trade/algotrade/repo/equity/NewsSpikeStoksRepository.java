package com.trade.algotrade.repo.equity;

import com.trade.algotrade.enitiy.equity.NewsSpikeStock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsSpikeStoksRepository extends MongoRepository<NewsSpikeStock, Long> {

    List<NewsSpikeStock> findByTradeDateGreaterThanEqual(LocalDateTime marketOpenDateTime);

}
