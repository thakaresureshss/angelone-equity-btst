package com.trade.algotrade.service;

import java.util.List;

import com.trade.algotrade.enitiy.NseHolidayEntity;
import com.trade.algotrade.enums.Segment;

/**
 * @author suresh.thakare
 */
public interface NseService {

	void updateNseHolidays();

	List<NseHolidayEntity> getHolidays(Segment segment);

	void clearOldHolidays();

	void fetchOpenInterestOfInstruments();

}