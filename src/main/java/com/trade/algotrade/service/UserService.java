package com.trade.algotrade.service;

import com.trade.algotrade.enitiy.UserEntity;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.request.UserRequest;
import com.trade.algotrade.response.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> getUsers();

    List<UserResponse> getUsersbyIds(List<String> userIds);

    UserResponse createUser(UserRequest userRequest) throws Exception;

    UserResponse updateUser(UserRequest userRequest) throws Exception;

    void updateAccessDetails(String userId, String sessionToken, String refreshToken, String feedToken);

    void deleteUser(Long userId);

    UserResponse getUserById(String userId);

    List<UserResponse> getAllActiveUsers();

    List<UserResponse> getAllActiveSegmentEnabledUsers(Segment segment);

    UserEntity getUserByUserId(String userId);

    UserEntity updateUserEntity(UserEntity userEntity);

    List<String> getStrategyAndSegmentEnabledActiveUserIds(String strategyName, String segment, List<String> users);

    UserResponse getActiveBrokerSystemUser(String broker);

    List<UserResponse> getAllActiveUsersByBroker(String broker);

    List<String> getStrategyAndSegmentEnabled(String strategyName, String segment);
}
