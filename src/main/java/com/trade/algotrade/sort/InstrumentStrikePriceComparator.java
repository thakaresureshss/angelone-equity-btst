package com.trade.algotrade.sort;

import java.util.Comparator;

import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import org.springframework.stereotype.Component;

import com.trade.algotrade.enitiy.KotakInstrumentMasterEntity;

@Component
public class InstrumentStrikePriceComparator implements Comparator<AngelOneInstrumentMasterEntity> {
	@Override
	public int compare(AngelOneInstrumentMasterEntity o1, AngelOneInstrumentMasterEntity o2) {
		return o1.getStrike().compareTo(o2.getStrike());
	}
}
