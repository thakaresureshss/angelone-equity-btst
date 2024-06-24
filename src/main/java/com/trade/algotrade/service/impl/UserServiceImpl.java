package com.trade.algotrade.service.impl;

import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.enitiy.UserEntity;
import com.trade.algotrade.enums.BrokerEnum;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.repo.UserRepository;
import com.trade.algotrade.request.UserRequest;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.service.mapper.UserMapper;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.SymmetricEncryption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SymmetricEncryption symmetricEncryption;

    private final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);


    @Override
    @Cacheable(value = "users")
    public List<UserResponse> getUsers() {
        logger.debug(" ****** [ UserServiceImpl ] [getUsers ] Called");
        List<UserEntity> userEntityList = userRepository.findAll();
        return userEntityList.stream().map(entity -> userMapper.userEntityToResponse(entity)).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse createUser(UserRequest userRequest) throws Exception {
        logger.debug(" ****** [ UserServiceImpl ] [createUser ] Called user Id {}", userRequest);
        Optional<UserEntity> userEntityOptional = userRepository.findByUserId(userRequest.getUserId());
        if (userEntityOptional.isPresent()) {
            throw new AlgotradeException(ErrorCodeConstants.USER_ID_ALREADY_EXIST_FAILED);
        }
        userRequest.setPassword(symmetricEncryption.encrypt(userRequest.getPassword()));
        UserEntity userRequestToEntity = userMapper.userRequestToEntity(userRequest);
        userRequestToEntity.setCreatedTime(LocalDateTime.now());
        userRequestToEntity.setUpdatedTime(LocalDateTime.now());
        UserEntity userEntity = userRepository.save(userRequestToEntity);
        return userMapper.userEntityToResponse(userEntity);
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateUser(UserRequest userRequest) throws Exception {
        logger.debug(" ****** [ UserServiceImpl ] [updateUser ] Called user Id {}", userRequest);
        Optional<UserEntity> optionalExistingUserEntity = userRepository.findById(userRequest.getId());
        if (optionalExistingUserEntity.isPresent()
                && !optionalExistingUserEntity.get().getPassword().equals(userRequest.getPassword())) {
            userRequest.setPassword(symmetricEncryption.encrypt(userRequest.getPassword()));
        }
        UserEntity userRequestToEntity = userMapper.userRequestToEntity(userRequest);
        userRequestToEntity.setUpdatedTime(LocalDateTime.now());
        UserEntity userEntity = userRepository.save(userRequestToEntity);
        return userMapper.userEntityToResponse(userEntity);
    }


    @Override
    @CacheEvict(value = {"users", "activeUsers"}, allEntries = true)
    public void updateAccessDetails(String userId, String sessionToken, String refreshToken, String feedToken) {
        logger.debug(" ****** [ UserServiceImpl ] [updateAccessDetails ] Called user Id {}", userId);
        Optional<UserEntity> optionalExistingUserEntity = userRepository.findByUserId(userId);
        if (optionalExistingUserEntity.isPresent()) {
            UserEntity userEntity = optionalExistingUserEntity.get();
            userEntity.setSessionToken(sessionToken);
            userEntity.setFeedToken(feedToken);
            userEntity.setRefreshToken(refreshToken);
            userEntity.setLastLogin(DateUtils.getCurrentDateTimeIst());
            userRepository.save(userEntity);
        }
    }

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long userId) {
        logger.debug(" ****** [ UserServiceImpl ] [deleteUser ] Called user Id {}", userId);
        Optional<UserEntity> userEntity = userRepository.findById(userId);
        if (userEntity.isEmpty()) {
            throw new AlgotradeException(ErrorCodeConstants.USER_NOT_EXIST_FAILED);
        }
        userRepository.delete(userEntity.get());
    }

    @Override
    //TODO Need to cache this method
    public List<UserResponse> getUsersbyIds(List<String> userIds) {
        logger.debug(" ****** [ UserServiceImpl ] [getUsersbyIds ] Called userIds {}", userIds);
        List<UserEntity> userEntityList = userRepository.findAllByUserIdIn(userIds);
        return userEntityList.stream().map(entity -> userMapper.userEntityToResponse(entity)).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "users", key = "#userId")
    public UserResponse getUserById(String userId) {
        logger.debug(" ****** [ UserServiceImpl ] [getUsersbyId ] Called User id {}", userId);
        Optional<UserEntity> userOptional = userRepository.findByUserId(userId);
        if (userOptional.isEmpty()) {
            throw new AlgotradeException(ErrorCodeConstants.USER_NOT_EXIST_FAILED);
        }
        UserEntity userEntity = userOptional.get();
        return userMapper.userEntityToResponse(userEntity);
    }

    @Override
    @Cacheable(value = "users")
    public List<UserResponse> getAllActiveUsers() {
        logger.debug(" ****** [ UserServiceImpl ] [getAllActiveUsers ] Called ");
        List<UserEntity> userEntityList = userRepository.findByActive(true);
        if (!CollectionUtils.isEmpty(userEntityList)) {
            return userEntityList.stream().map(entity -> userMapper.userEntityToResponse(entity)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    @Cacheable(value = "users", key = "#segment ")
    public List<UserResponse> getAllActiveSegmentEnabledUsers(Segment segment) {
        logger.debug(" Get all users by Segment {}",
                segment.toString());
        List<UserResponse> allActiveUsers = getAllActiveUsersByBroker(BrokerEnum.ANGEL_ONE.toString());
        if (!CollectionUtils.isEmpty(allActiveUsers)) {
            return allActiveUsers.stream().filter(ue -> ue.getEnabledSegments().contains(segment.toString())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    @Cacheable(value = "userEntities", key = "#userId")
    public UserEntity getUserByUserId(String userId) {
        logger.debug(" ****** [ UserServiceImpl ] [getUsersbyId ] Called User id {}", userId);
        Optional<UserEntity> userEntityOptional = userRepository.findByUserId(userId);
        return userEntityOptional.orElse(null);
    }

    @Override
    public UserEntity updateUserEntity(UserEntity userEntity) {
        return userRepository.save(userEntity);
    }

    @Override
    //@Cacheable(value = "users", key = "#strategyName#segment#users.hashCode()")
    public List<String> getStrategyAndSegmentEnabledActiveUserIds(String strategyName, String segment, List<String> users) {
        List<UserEntity> eligibleUsers = userRepository.findByActiveAndEnabledStrategiesInAndEnabledSegmentsInAndBrokerAndUserIdIn(true, strategyName, segment, BrokerEnum.ANGEL_ONE.toString(), users);

        if (!CollectionUtils.isEmpty(eligibleUsers)) {
            return eligibleUsers.stream().map(UserEntity::getUserId).collect(Collectors.toList());
        }

        return null;
    }

    @Override
    @Cacheable(value = "activeUsers", key = "#brokerName")
    public UserResponse getActiveBrokerSystemUser(String brokerName) {
        logger.info(" Get Active users By broker {}", brokerName);
        Optional<UserEntity> systemUserForBroker = userRepository.findByActiveAndSystemUserAndBroker(true, true, brokerName);
        if (systemUserForBroker.isPresent()) {
            return userMapper.userEntityToResponse(systemUserForBroker.get());
        }
        return null;
    }


    @Override
    public List<UserResponse> getAllActiveUsersByBroker(String brokerName) {
        List<UserEntity> allActiveBrokerUsers = userRepository.findByActiveAndBroker(true, brokerName);
        if (!CollectionUtils.isEmpty(allActiveBrokerUsers)) {
            return allActiveBrokerUsers.stream().map(user -> userMapper.userEntityToResponse(user)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    @Cacheable(value = "users", key = "#strategyName#segment")
    public List<String> getStrategyAndSegmentEnabled(String strategyName, String segment) {
        List<UserEntity> eligibleUsers = userRepository.findByActiveAndEnabledStrategiesInAndEnabledSegmentsInAndBroker(true, strategyName, segment, BrokerEnum.ANGEL_ONE.toString());

        if (!CollectionUtils.isEmpty(eligibleUsers)) {
            return eligibleUsers.stream().map(UserEntity::getUserId).collect(Collectors.toList());
        }
        return null;
    }
}
