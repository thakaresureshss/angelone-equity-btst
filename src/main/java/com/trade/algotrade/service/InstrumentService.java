package com.trade.algotrade.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import com.trade.algotrade.enitiy.AngelKotakInstrumentMappingEntity;
import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.enitiy.KotakInstrumentMasterEntity;
import com.trade.algotrade.response.InstrumentResponse;
import org.springframework.cache.annotation.CacheEvict;

/**
 * @author suresh.thakare
 */
public interface InstrumentService {

	void downlodInstruments();

	List<AngelOneInstrumentMasterEntity> downloadAndProcessAngelInstrument();

	List<AngelOneInstrumentMasterEntity> processAngeloneInstruments(List<LinkedHashMap> linkedHashMaps);

	List<AngelOneInstrumentMasterEntity> getAllInstruments();

	AngelOneInstrumentMasterEntity findByInstrumentName(String bankNiftyIndex);

	AngelOneInstrumentMasterEntity getInstrumentByToken(Long instrumentToken);


	@CacheEvict(cacheNames = "angelOneInstrumentMasters", allEntries = true)
	void deleteOldAngelOneInstruments();

	void saveAngelOneInstruments(List<AngelOneInstrumentMasterEntity> angelOneInstruements);

	void saveAngelOneAndKotakInstrumentMapping(List<KotakInstrumentMasterEntity> instrumentMasterEntities, List<AngelOneInstrumentMasterEntity> angelOneInstruements);

	List<AngelKotakInstrumentMappingEntity> getAngelKotakMapping();

	Long getKotakInstrumentByAngel(Long instrumentToken);

	Long getAngelInstrumentByKotak(Long instrumentToken);

	Optional<AngelOneInstrumentMasterEntity> getAngelOneInstrument(String instrumentName);

	List<AngelOneInstrumentMasterEntity> getAllAngelInstrumentsByStrike(List<Long> strikelist);

	InstrumentResponse getInstrumentByTokenTest(Long instrumentToken);
}
