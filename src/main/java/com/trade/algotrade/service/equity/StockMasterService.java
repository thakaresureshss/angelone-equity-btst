package com.trade.algotrade.service.equity;

import com.trade.algotrade.enitiy.equity.StockMaster;
import com.trade.algotrade.enitiy.equity.TopGainerLooserEntity;

import java.util.List;

/**
 * @author suresh.thakare
 */
public interface StockMasterService {

	List<StockMaster> getAllStocks();

	StockMaster getStockByName(String stockName);

	void deleteStock(String stockName);

	StockMaster createEquity(String stockName, StockMaster stock);

	void updateAll(List<StockMaster> allStocks);

	void addStock(StockMaster stockMaster);

	List<TopGainerLooserEntity> findTopLoosers();

	List<TopGainerLooserEntity> findTopGainers();

	List<TopGainerLooserEntity> findAndSaveTopMovers();

	void updateDayClose();

	void updateDayOpen();

	List<TopGainerLooserEntity> getTopTenMoves();

	List<TopGainerLooserEntity> getTopTenGainer();

	List<TopGainerLooserEntity> getTopTenLooser();

	void findNewsSpikeStockAndSave();

	void updateNewsSpikeStockGain();

	void readNseStockCsv(String fileNames, Integer indexStockCount);

	void processTopMoversEquityOrder(List<TopGainerLooserEntity> eligibleTopMovers);
}