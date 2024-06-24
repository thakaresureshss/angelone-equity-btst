package com.trade.algotrade.sort;

import java.util.Comparator;

import com.trade.algotrade.enitiy.equity.StockMaster;
import com.trade.algotrade.enitiy.equity.TopGainerLooserEntity;
import com.trade.algotrade.response.TradeResponse;

public class SortUtils {
    public static Comparator<StockMaster> percentageChangeAscComparator() {
        return (sm1, sm2) -> {
            if (sm1.getChangePercent() == null || sm2.getChangePercent() == null) {
                return -1;
            }
            return sm1.getChangePercent().compareTo(sm2.getChangePercent());
        };
    }

    public static Comparator<StockMaster> percentageChangeDescComparator() {
        return (sm1, sm2) -> {
            if (sm1.getChangePercent() == null || sm2.getChangePercent() == null) {
                return -1;
            }
            return sm2.getChangePercent().compareTo(sm1.getChangePercent());
        };
    }

    public static Comparator<TopGainerLooserEntity> percentageChangeComparatorTopGainerLoosers() {
        return Comparator.comparing(TopGainerLooserEntity::getChangePercent);
    }

    public static Comparator<TradeResponse> uniqueTrendCountComparator() {
        return (sm1, sm2) -> {
            if (sm1.getUniqueTrendCount() == null || sm2.getUniqueTrendCount() == null) {
                return -1;
            }
            return sm1.getUniqueTrendCount().compareTo(sm2.getUniqueTrendCount());
        };
    }


    public static Comparator<TradeResponse> durationCountComparator() {
        return (sm1, sm2) -> {
            if (sm1.getTradeDuration() == null || sm2.getTradeDuration() == null) {
                return -1;
            }
            return sm1.getUniqueTrendCount().compareTo(sm2.getUniqueTrendCount());
        };
    }
}
