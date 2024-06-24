package com.trade.algotrade.repo.filter;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface FilterableRepository<T> {

    List<T> findAllByFiter(Class<T> classType, FilterBuilder filterBuilder);

    default Query createCriteriaQuery(FilterBuilder filterBuilder) {
        Query query = new Query();
        Map<String, Criteria> criteriaMap = new HashMap<>();
        for (FilterBuilder.FilterCondition filterCondition : filterBuilder.getFilterConditions()) {
            switch (filterCondition.getOperation().toString()) {
                case "eq":
                    criteriaMap.put(filterCondition.getKey(), Criteria.where(filterCondition.getKey()).is(filterCondition.getValue()));
                    break;
                case "lte":
                    criteriaMap.put(filterCondition.getKey(), Criteria.where(filterCondition.getKey()).lte(filterCondition.getValue()));
                    break;
                case "gte":
                    criteriaMap.put(filterCondition.getKey(), Criteria.where(filterCondition.getKey()).gte(filterCondition.getValue()));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown operator: " + filterCondition.operation);
            }
        }
        criteriaMap.values().forEach(query::addCriteria);
        return query;
    }
}
