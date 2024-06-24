package com.trade.algotrade.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.trade.algotrade.enitiy.UserEntity;

@Repository
public interface UserRepository extends MongoRepository<UserEntity, Long> {

    List<UserEntity> findAllByUserIdIn(List<String> userIds);

    Optional<UserEntity> findByUserId(String userId);

    List<UserEntity> findByActive(Boolean active);

    List<UserEntity> findByActiveAndEnabledStrategiesInAndEnabledSegmentsInAndBrokerAndUserIdIn(boolean active, String strategyName, String segment, String broker, List<String> users);

    Optional<UserEntity> findByActiveAndSystemUserAndBroker(boolean active,boolean systemUser,  String brokerName);

    List<UserEntity> findByActiveAndBroker(boolean b, String brokerName);

    List<UserEntity> findByActiveAndEnabledStrategiesInAndEnabledSegmentsInAndBroker(boolean b, String strategyName, String segment, String string);
}
