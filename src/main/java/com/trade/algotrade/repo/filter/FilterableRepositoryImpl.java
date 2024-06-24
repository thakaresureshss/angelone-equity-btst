package com.trade.algotrade.repo.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FilterableRepositoryImpl<T> implements FilterableRepository<T>{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<T> findAllByFiter(Class<T> classType, FilterBuilder filterBuilder) {
        Query query = createCriteriaQuery(filterBuilder);
        return mongoTemplate.query(classType).matching(query).all();
    }
}
