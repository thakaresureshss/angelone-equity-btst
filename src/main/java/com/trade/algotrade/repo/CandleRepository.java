package com.trade.algotrade.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.CandleEntity;

@Repository
public interface CandleRepository extends MongoRepository<CandleEntity, String> {

	List<CandleEntity> findByStockSymbolIgnoreCase(String stockSymbol);

	Optional<CandleEntity> findByStockSymbolOrderByModifiedTime(String stockSymbol);

	List<CandleEntity> findByStockSymbolIgnoreCaseOrderByStartTimeDesc(String stockSymbol);
}
