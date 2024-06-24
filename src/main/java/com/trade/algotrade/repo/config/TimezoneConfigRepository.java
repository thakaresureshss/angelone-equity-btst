package com.trade.algotrade.repo.config;

import com.trade.algotrade.enitiy.CandleEntity;
import com.trade.algotrade.enitiy.config.TimezoneConfigEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimezoneConfigRepository extends MongoRepository<TimezoneConfigEntity, String> {

    void deleteByTimezoneKeyAndTimezone(String configKey, String timezone);

    String findByTimezoneKeyAndTimezone(String marketOpenTime, String timezone);
}
