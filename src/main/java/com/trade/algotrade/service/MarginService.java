package com.trade.algotrade.service;

import java.math.BigDecimal;

import com.trade.algotrade.client.angelone.response.user.MarginData;
import com.trade.algotrade.client.kotak.dto.MarginDto;

public interface MarginService {

	BigDecimal getMargin(String userId);

	void getMarginForAllUser();

	void addMarginInDB(MarginData marginData, String userId);

	MarginData getMarginFromKotak(String userId);

	void updateMarginInDB(String userId, BigDecimal updatedMargin);

}
