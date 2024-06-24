package com.trade.algotrade.enitiy;

import com.trade.algotrade.enums.Segment;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NseHolidayKey {

	private String holidayDate;

	private Segment segment;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NseHolidayKey other = (NseHolidayKey) obj;
		if (holidayDate == null) {
			if (other.holidayDate != null)
				return false;
		} else if (!holidayDate.equals(other.holidayDate))
			return false;
		return segment == other.segment;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((holidayDate == null) ? 0 : holidayDate.hashCode());
		result = prime * result + ((segment == null) ? 0 : segment.hashCode());
		return result;
	}

}