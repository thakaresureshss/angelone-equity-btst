package com.trade.algotrade.service;

public interface MongoDumpService {
	
	public void restoreStaticTables();

	public void takeDumpOfStaticTables();
	
	public void restoreStaticTable(String strategy);

}
