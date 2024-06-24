package com.trade.algotrade.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trade.algotrade.utils.TradeUtils;

@ExtendWith(MockitoExtension.class)
class TradeUtilsTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testXPercentOfY() throws InterruptedException {
		BigDecimal from = new BigDecimal(5);
		BigDecimal to = new BigDecimal(100);
		BigDecimal xPercentOfY = TradeUtils.getXPercentOfY(from, to);
		System.out.println(xPercentOfY);
		assertTrue(xPercentOfY.compareTo(new BigDecimal(5)) == 0);
	}

	@Test
	void testXPercentOfYNegative() throws InterruptedException {
		BigDecimal from = new BigDecimal(-5);
		BigDecimal to = new BigDecimal(100);
		BigDecimal xPercentOfY = TradeUtils.getXPercentOfY(from, to);
		System.out.println(xPercentOfY);
		assertTrue(xPercentOfY.compareTo(new BigDecimal(-5)) == 0);
	}

	@Test
	void testGetPercent() throws InterruptedException {
		BigDecimal from = new BigDecimal(100);
		BigDecimal to = new BigDecimal(110);
		BigDecimal percent = TradeUtils.getPercent(from, to);
		System.out.println(percent);
		assertTrue(percent.compareTo(new BigDecimal(10)) == 0);
	}

	@Test
	void testGetPercentNegative() throws InterruptedException {
		BigDecimal from = new BigDecimal(100);
		BigDecimal to = new BigDecimal(50);
		BigDecimal percent = TradeUtils.getPercent(from, to);
		System.out.println(percent);
		assertTrue(percent.compareTo(new BigDecimal(-50)) == 0);
	}
}
