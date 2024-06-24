
package com.trade.algotrade.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.trade.algotrade.service.NseService;
import com.trade.algotrade.utils.CommonUtils;

@Component
@EnableAsync
public class NseScheduler {

	@Autowired
	CommonUtils commonUtils;

	@Autowired
	NseService nseService;

	private final Logger logger = LoggerFactory.getLogger(NseScheduler.class);

	@Async
	@Scheduled(cron = "${scheduler.holiday.download.interval}")
	public void downloadHolidayList() {
		logger.info(" ******[NseScheduler][downloadHolidayList] Called ********");
		nseService.updateNseHolidays();
		logger.info(" ****** [NseScheduler][downloadHolidayList] Completed ********");
	}

	@Async
	@Scheduled(cron = "${scheduler.holiday.download.interval}")
	public void clearOldHolidays() {
		logger.info(" ******[NseScheduler][clearOldHolidays] Called ********");
		nseService.clearOldHolidays();
		logger.info(" ****** [NseScheduler][clearOldHolidays] Completed ********");
	}

	@Async
	//@Scheduled(cron= "${scheduler.open-interest.interval}")
	public void fetchOpenInterestOfInstruments(){
		logger.info(" ******[NseScheduler][fetchOpenInterestOfInstruments] Called ********");
		nseService.fetchOpenInterestOfInstruments();
		logger.info(" ****** [NseScheduler][fetchOpenInterestOfInstruments] Completed ********");
	}

}