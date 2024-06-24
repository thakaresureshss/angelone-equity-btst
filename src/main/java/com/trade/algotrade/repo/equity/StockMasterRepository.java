package com.trade.algotrade.repo.equity;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.equity.StockMaster;

@Repository
public interface StockMasterRepository extends MongoRepository<StockMaster, Long> {

	Optional<StockMaster> findByStockSymbol(String stockSymbol);

	Optional<StockMaster> findByStockName(String stockName);

	void deleteByStockName(String stockName);

}
