package com.trade.algotrade.repo;

import java.util.List;
import java.util.Optional;

import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.KotakInstrumentMasterEntity;

@Repository
public interface InstrumentMasterRepo extends MongoRepository<AngelOneInstrumentMasterEntity, Long> {

	Optional<AngelOneInstrumentMasterEntity> findByInstrumentNameIgnoreCase(String bankNiftyIndex);

	AngelOneInstrumentMasterEntity findByInstrumentToken(Long instrumentToken);
}