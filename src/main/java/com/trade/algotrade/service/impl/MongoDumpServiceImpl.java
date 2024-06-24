package com.trade.algotrade.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.trade.algotrade.service.MongoDumpService;
import com.trade.algotrade.service.StrategyService;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.utils.MongoDumpUtils;

@Service
public class MongoDumpServiceImpl implements MongoDumpService {
	
	private final Logger log = LoggerFactory.getLogger(MongoDumpServiceImpl.class);
	
	@Autowired
    private UserService userService;
	
	@Autowired
    private StrategyService strategyService;
	
	@Autowired
	private MongoDumpUtils dumpUtils;


	@Override
	public void restoreStaticTables() {
		if (userService.getUsers().isEmpty()) {
			log.info(" ****** [ MongoDumpServiceImpl ] [restoreStaticTables ] restoring user_master Started ********");
			dumpUtils.restoreDBDumps("user_master");
		}
		if (strategyService.getAllStrategies().isEmpty()) {
			log.info(" ****** [ MongoDumpServiceImpl ] [restoreStaticTables ] restoring strategy_master Started ********");
			dumpUtils.restoreDBDumps("strategy_master");
		}
	}

	@Override
	public void takeDumpOfStaticTables() {
		log.info(" ****** [ MongoDumpServiceImpl ] [takeDumpOfStaticTables ] Started ********");
		dumpUtils.dumpDBColletions();
	}
	
	@Override
	public void restoreStaticTable(String strategy) {
		log.info(" ****** [ MongoDumpServiceImpl ] [restoreStaticTable ] restoring {} Started ********", strategy);
		dumpUtils.restoreDBDumps(strategy);
	}

}
