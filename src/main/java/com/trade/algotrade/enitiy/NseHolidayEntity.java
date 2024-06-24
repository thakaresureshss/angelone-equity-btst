package com.trade.algotrade.enitiy;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.trade.algotrade.enums.Segment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@Document("nse_holidays")
@ToString
@AllArgsConstructor
@NoArgsConstructor
@CompoundIndex(name = "nse_holidays_idx", def = "{'holidayDate': 1, 'segment': 1}", unique = true)
public class NseHolidayEntity {

	// @Id
	// private NseHolidayKey id;

	private String holidayDate;

	private Segment segment;

	private String holidayDay;

	private String description;

	private Integer srNo;

}