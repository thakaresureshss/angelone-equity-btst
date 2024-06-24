package com.trade.algotrade.repo;

import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AngelOneInstrumentMasterRepo extends MongoRepository<AngelOneInstrumentMasterEntity, Long> {

    Optional<AngelOneInstrumentMasterEntity> findByInstrumentNameIgnoreCase(String bankNiftyIndex);

    AngelOneInstrumentMasterEntity findByInstrumentToken(Long instrumentToken);

    List<AngelOneInstrumentMasterEntity> findByStrikeIn(List<Long> strikeList);
}