package com.trade.algotrade.repo.filter;

import com.trade.algotrade.enums.CriteriaFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class FilterBuilder {

    private final List<FilterCondition> filterConditions = new ArrayList<>();

    public void addFilter(String key, CriteriaFilter operation, Object value){
        filterConditions.add(new FilterCondition(key,operation,value));
    }

    public List<FilterCondition> getFilterConditions(){
        return filterConditions;
    }

    @Data
    @AllArgsConstructor
    public class FilterCondition{
       String key;
       CriteriaFilter operation;
       Object value;
    }
}
