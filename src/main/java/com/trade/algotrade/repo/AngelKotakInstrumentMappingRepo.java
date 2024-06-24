package com.trade.algotrade.repo;

import com.trade.algotrade.enitiy.AngelKotakInstrumentMappingEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AngelKotakInstrumentMappingRepo extends MongoRepository<AngelKotakInstrumentMappingEntity, Long> {

    Optional<AngelKotakInstrumentMappingEntity> findByAngelInstrument(Long instrumentToken);

    Optional<AngelKotakInstrumentMappingEntity> findByKotakInstrument(Long instrumentToken);

}