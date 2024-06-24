package com.trade.algotrade.client.nse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service("nseClient")
public class NseClientImpl implements NseClient {

	private static final Logger logger = LoggerFactory.getLogger(NseClientImpl.class);

	@Value("${application.nseClient.url}")
	private String nseClientBaseUrl;

	@Autowired
	private RestTemplate restTemplate;

	private String getBaseUrl() {
		return nseClientBaseUrl;

	}

	@Cacheable("holidays")
	public NseHolidaysResponse getHolidays() {
		logger.debug("******* [NseClientImpl][getHolidayList] Started : Getting holiday list from Flyers");
		final HttpHeaders headers = new HttpHeaders();
		headers.set("Host", "<calculated when request is sent>");
		headers.set("User-Agent", "PostmanRuntime/7.29.2");
		final HttpEntity<String> request = new HttpEntity<>(headers);
		String url = getBaseUrl() + "/api/holiday-master?type=trading";
		try {
			return restTemplate.exchange(url, HttpMethod.GET, request, NseHolidaysResponse.class).getBody();
		} catch (Exception e) {
			logger.error("[** ERROR **] Exception {}", e.getMessage());
		}
		logger.debug("******* [NseClientImpl][getHolidayList] Completed : Getting holiday list from Flyers");
		return null;
	}
}
