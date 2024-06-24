package com.trade.algotrade.repo;

import com.trade.algotrade.enitiy.TradeEntity;
import com.trade.algotrade.enitiy.TradeHistoryEntity;
import com.trade.algotrade.repo.filter.FilterBuilder;
import com.trade.algotrade.repo.filter.FilterableRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

public interface
TradeHistoryRepository extends MongoRepository<TradeHistoryEntity, String>, FilterableRepository<TradeHistoryEntity> {

}
