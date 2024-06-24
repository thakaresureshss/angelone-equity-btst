package com.trade.algotrade.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.algotrade.enitiy.NseHolidayEntity;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.utils.CommonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonUtilsTest {

	@InjectMocks
	CommonUtils commonUtils;

	@Mock
	NseService nseClient;

	List<NseHolidayEntity> holidayList;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() throws Exception {
		objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		File resouce = new ClassPathResource("responseJson/holidays.json").getFile();
		String fleContent = new String(Files.readAllBytes(resouce.toPath()));
		holidayList = objectMapper.readValue(fleContent, new TypeReference<List<NseHolidayEntity>>() {
		});
	}

//	@Test
	void getLocalHostIp() {
		String hostIp = CommonUtils.getHostIp();
		assertEquals(hostIp, "192.168.0.139");
	}

	@Test
	void isExpiryOnHoliday() throws ParseException {
		when(nseClient.getHolidays(Segment.EQ)).thenReturn(holidayList);
		Date ramNavmiThursdayHoliday = new SimpleDateFormat("yyyy-MM-dd").parse("2023-03-30");
		boolean expiryOnHoliday = commonUtils.isExpiryOnHoliday(ramNavmiThursdayHoliday);
		assertTrue(expiryOnHoliday);
	}

	@Test
	void isExpiryOnNonHoliday() throws ParseException {
		when(nseClient.getHolidays(Segment.EQ)).thenReturn(holidayList);
		Date nonHoliday = new SimpleDateFormat("yyyy-MM-dd").parse("2023-03-29");
		boolean expiryOnHoliday = commonUtils.isExpiryOnHoliday(nonHoliday);
		assertFalse(expiryOnHoliday);
	}

}
