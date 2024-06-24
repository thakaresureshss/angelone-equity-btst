package com.trade.algotrade.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.NseHolidayEntity;
import com.trade.algotrade.enitiy.NseHolidayKey;
import com.trade.algotrade.enums.Segment;

@Repository
public interface NseHolidayRepo extends MongoRepository<NseHolidayEntity, NseHolidayKey> {

	List<NseHolidayEntity> findBySegment(Segment segment);

}
