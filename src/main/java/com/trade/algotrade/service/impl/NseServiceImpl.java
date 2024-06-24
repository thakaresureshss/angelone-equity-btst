package com.trade.algotrade.service.impl;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.trade.algotrade.client.angelone.AngelOneClient;
import com.trade.algotrade.client.angelone.enums.ExchangeType;
import com.trade.algotrade.client.angelone.response.feed.QouteResponse;
import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.dto.LtpSuccess;
import com.trade.algotrade.client.kotak.response.LtpResponse;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.enitiy.OpenOrderEntity;
import com.trade.algotrade.enums.OrderStatus;
import com.trade.algotrade.service.InstrumentService;
import com.trade.algotrade.utils.TradeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.trade.algotrade.client.nse.NseClient;
import com.trade.algotrade.client.nse.NseHolidayDto;
import com.trade.algotrade.client.nse.NseHolidaysResponse;
import com.trade.algotrade.enitiy.NseHolidayEntity;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.repo.NseHolidayRepo;
import com.trade.algotrade.service.NseService;

@Service
public class NseServiceImpl implements NseService {

	private final Logger logger = LoggerFactory.getLogger(NseServiceImpl.class);

	@Autowired
	NseClient nseClient;

	@Autowired
	KotakClient kotakClient;

	@Autowired
	AngelOneClient angelOneClient;


	@Autowired
	NseHolidayRepo nseHolidayRepo;

	@Autowired
	InstrumentService instrumentService;

	@Override
	public void updateNseHolidays() {
		deleteAllHolidays();
		NseHolidaysResponse holidays = nseClient.getHolidays();
		if (Objects.nonNull(holidays)) {
			List<NseHolidayEntity> holidayList = new ArrayList<>();
			ArrayList<NseHolidayDto> equities = holidays.getEquities();

			if (!CollectionUtils.isEmpty(equities)) {
				equities.forEach(e -> {
					Segment eq = Segment.EQ;
					NseHolidayEntity nse = mapNseResponseToEntity(e, eq);
					holidayList.add(nse);
				});

			}
			ArrayList<NseHolidayDto> equityDerivatives = holidays.getEquityDerivatives();
			if (!CollectionUtils.isEmpty(equityDerivatives)) {
				equityDerivatives.forEach(e -> {
					Segment segment = Segment.FNO;
					NseHolidayEntity nse = mapNseResponseToEntity(e, segment);
					holidayList.add(nse);
				});
			}
			saveHolidays(holidayList);
		}
	}

	@CacheEvict(value = "holidays", allEntries = true)
	private void saveHolidays(List<NseHolidayEntity> holidayList) {
		nseHolidayRepo.saveAll(holidayList);
	}

	@CacheEvict(value = "holidays", allEntries = true)
	private void deleteAllHolidays() {
		nseHolidayRepo.deleteAll();
	}

	private NseHolidayEntity mapNseResponseToEntity(NseHolidayDto e, Segment segment) {
		NseHolidayEntity nse = new NseHolidayEntity();
		nse.setSrNo(e.getSrNo());
		nse.setDescription(e.getDescription());
		nse.setHolidayDay(e.getWeekDay());
		nse.setHolidayDate(e.getTradingDate());
		nse.setSegment(segment);
		return nse;
	}

	@Override
	@Cacheable(value = "holidays", key = "#segment")
	public List<NseHolidayEntity> getHolidays(Segment segment) {
		return nseHolidayRepo.findBySegment(segment);
	}

	@Override
	public void clearOldHolidays() {
		deleteAllHolidays();
	}

	@Override
	public void fetchOpenInterestOfInstruments() {
		logger.info("OPEN INTEREST SCHEDULER CALLED");
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		Timer timer = new Timer();
		String oiMonitorIntervalInMinutes = "15";
		long interval = Long.parseLong(oiMonitorIntervalInMinutes) * 1000 * 60;
		long delay = 1000;
		List<String> oiTokens = new ArrayList<>();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					Optional<AngelOneInstrumentMasterEntity> optionalAngelOneInstrument = instrumentService.getAngelOneInstrument(Constants.INDIA_VIX_INDEX);
					if(optionalAngelOneInstrument.isPresent()){
						logger.info("Getting LTP for INDIA VIX Called");
						QouteResponse ltp = angelOneClient.getQuote(ExchangeType.NSE, List.of(String.valueOf(optionalAngelOneInstrument.get().getInstrumentToken())));
						if (ltp != null && ltp.getData() != null) {
							TradeUtils.indiaVixStrike = ltp.getData().getFetched().stream().findFirst().get().getLtp();
						}
					}

					//Fetch Open Interest and store in DB against the instruments after configured interval.
//					if(oiTokens.isEmpty()){
//						optionalAngelOneInstrument = instrumentService.getAngelOneInstrument(Constants.BANK_NIFTY_INDEX);
//						if(optionalAngelOneInstrument.isPresent()) {
//							LtpResponse ltp = kotakClient.getLtp(String.valueOf(optionalAngelOneInstrument.get().getInstrumentToken()));
//							if (ltp != null && ltp.getSuccess() != null && !CollectionUtils.isEmpty(ltp.getSuccess())) {
//								Optional<LtpSuccess> instrumentLtpOptional = ltp.getSuccess().stream().findFirst();
//								BigDecimal ltpPrice = instrumentLtpOptional.get().getLastPrice();
//								//47820 -
//								List<Integer> closestStrikePrices = TradeUtils.getClosestStrikePrices(ltpPrice);
//								Integer strikePrice =0;
//								int openInterestRange = 6;
//								List<Long> strikePriceList = new ArrayList<>();
//								for(int i =0; i < openInterestRange; i++){
//									if(i > 0){
//										strikePrice = strikePrice + 100;
//									}
//									strikePriceList.add((long) (closestStrikePrices.get(0) + strikePrice));
//								}
//								for(int i =0; i < openInterestRange; i++){
//									if(i > 0){
//										strikePrice = strikePrice - 100;
//									}
//									strikePriceList.add((long)(closestStrikePrices.get(1) + strikePrice));
//								}
//								List<AngelOneInstrumentMasterEntity> allAngelInstrumentsByStrike = instrumentService.getAllAngelInstrumentsByStrike(strikePriceList);
//							}
//						}
//					}
				} catch (Exception ex) {
					future.completeExceptionally(ex);
				}
			}
		}, delay, interval);
		logger.info("OPEN INTEREST SCHEDULER COMPLETED");
	}

}
