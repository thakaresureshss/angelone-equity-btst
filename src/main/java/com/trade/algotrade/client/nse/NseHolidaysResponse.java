package com.trade.algotrade.client.nse;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class NseHolidaysResponse {

	@JsonProperty("CM")
	ArrayList<NseHolidayDto> equities = new ArrayList<>();

	@JsonProperty("FO")
	ArrayList<NseHolidayDto> equityDerivatives = new ArrayList<>();

	/*
	 * ArrayList<NseHolidayDto> CD = new ArrayList<NseHolidayDto>();
	 * ArrayList<NseHolidayDto> CM = new ArrayList<NseHolidayDto>();
	 * ArrayList<NseHolidayDto> CMOT = new ArrayList<NseHolidayDto>();
	 * ArrayList<NseHolidayDto> COM = new ArrayList<NseHolidayDto>();
	 * ArrayList<NseHolidayDto> IRD = new ArrayList<NseHolidayDto>();
	 * ArrayList<NseHolidayDto> MF = new ArrayList<NseHolidayDto>();
	 * ArrayList<NseHolidayDto> NDM = new ArrayList<NseHolidayDto>();
	 * ArrayList<NseHolidayDto> NTRP = new ArrayList<NseHolidayDto>();
	 * ArrayList<NseHolidayDto> SLBS = new ArrayList<NseHolidayDto>();
	 */

}
