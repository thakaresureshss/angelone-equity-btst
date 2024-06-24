package com.trade.algotrade.repo.equity;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.equity.TopGainerLooserEntity;
import com.trade.algotrade.enums.MoversType;

@Repository
public interface TopGainerLoosersRepository extends MongoRepository<TopGainerLooserEntity, Long> {

	List<TopGainerLooserEntity> findByType(MoversType looser);

}
