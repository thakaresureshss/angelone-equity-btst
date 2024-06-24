package com.trade.algotrade.repo;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.InstrumentWatchEntity;

@Repository
public interface InstrumentWatchMasterRepo extends MongoRepository<InstrumentWatchEntity, Long> {

    Optional<InstrumentWatchEntity> findByInstrumentToken(Long instrument);

}