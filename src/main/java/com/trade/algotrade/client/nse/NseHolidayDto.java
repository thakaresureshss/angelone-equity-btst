package com.trade.algotrade.client.nse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NseHolidayDto {

	@JsonProperty("tradingDate")
	private String tradingDate;

	@JsonProperty("weekDay")
	private String weekDay;

	@JsonProperty("description")
	private String description;

	@JsonProperty("Sr_no")
	private Integer srNo;

}