package com.trade.algotrade.client.angelone.service;

import com.trade.algotrade.client.angelone.request.GetCandleRequest;
import com.trade.algotrade.enitiy.CandleEntity;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface AngelCandleService {

    List<CandleEntity> getCandleData(GetCandleRequest candleRequest);

    BigDecimal latestTrendFinder();

    Collection<CandleEntity> findCandlesBySymbolsDescending(String bankNiftyIndex);

    void clearTrendData();
}
