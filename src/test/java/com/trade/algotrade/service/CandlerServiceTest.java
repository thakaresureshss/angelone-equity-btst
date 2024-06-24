package com.trade.algotrade.service;

import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.repo.CandleRepository;
import com.trade.algotrade.service.impl.CandleServiceImpl;
import com.trade.algotrade.utils.TradeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandlerServiceTest {

	@InjectMocks
	CandleService candleService = new CandleServiceImpl();

	@Mock
	CandleRepository candleRepository;

	private static final Logger logger = LoggerFactory.getLogger(CandlerServiceTest.class);

	@BeforeEach
	void setUp() throws Exception {
		TradeUtils.getNiftyBankCandle().clear();
	}

	//@Test

	void buildCandleTestWithSaveMock() throws InterruptedException {
		ReflectionTestUtils.setField(candleService, "candleTimeFrame", 1);
		Thread.sleep(10l);
		CandleEntity candleEntity = new CandleEntity();
		when(candleRepository.save(any())).thenReturn(candleEntity);
		CandleEntity buildCandle = null;
		int max = 42300;
		int min = 42150;
		long start = System.currentTimeMillis();
		long end = start + (2 * 60 * 1000); // 2 Minute *60 Second * 1 Second(1000millis)
		while (System.currentTimeMillis() < end) {
			Random random = new Random();
			int ltp = random.nextInt(max - min) + min;
			logger.info("Building Candle for {} : LTP {}", Constants.BANK_NIFTY_INDEX, ltp);
			buildCandle = candleService.buildCandleBankNiftyCandles(Constants.BANK_NIFTY_INDEX, new BigDecimal(ltp));
			Thread.sleep(10l);
		}
		assertTrue(buildCandle != null);
		long count = TradeUtils.getNiftyBankCandle().values().stream().count();
		assertTrue(count == 2);
		// logger.info("Candle Data : {}", TradeUtils.getNiftyBankCandle().values());
		// logger.info("Candle Data Counts : {}", count);
	}


	@Disabled
	void buildCandleTestWithParameters() throws InterruptedException {
		ReflectionTestUtils.setField(candleService, "candleTimeFrame", 1);
		Integer open = null;
		Integer close = null;
		Integer high = null;
		Integer low = null;
		CandleEntity buildCandle = null;
		int max = 42300;
		int min = 42150;
		for (int i = 0; i < 20; i++) {
			synchronized (this) {
				Random random = new Random();
				int ltp = random.nextInt(max - min) + min;
				System.out.println("OPEN " + open);
				if (i == 0) {
					open = ltp;
					high = ltp;
					low = ltp;
				}
				if (i == 19) {
					close = ltp;
				}
				if (high < ltp) {
					high = ltp;
				}
				if (ltp < low) {
					low = ltp;
				}
				System.out.println("OPEN " + open);
				Thread.sleep(1l);
				buildCandle = candleService.buildCandleBankNiftyCandles(Constants.BANK_NIFTY_INDEX, new BigDecimal(ltp));
			}
		}
		assertTrue(buildCandle != null);
		assertTrue(buildCandle.getLow().intValue() == low);
		assertTrue(buildCandle.getClose().intValue() == close);
		assertTrue(buildCandle.getHigh().intValue() == high);
		assertTrue(buildCandle.getOpen().intValue() == open);
	}
}
