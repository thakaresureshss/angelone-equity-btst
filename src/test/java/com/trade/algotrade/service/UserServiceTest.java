package com.trade.algotrade.service;

import com.trade.algotrade.AlgoTradeTestDtoFactory;
import com.trade.algotrade.enitiy.UserEntity;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.repo.UserRepository;
import com.trade.algotrade.request.UserRequest;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.impl.UserServiceImpl;
import com.trade.algotrade.service.mapper.UserMapper;
import com.trade.algotrade.utils.SymmetricEncryption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    UserService userService = new UserServiceImpl();


    private AlgoTradeTestDtoFactory algoTradeTestDtoFactory;


    @Mock
    UserRepository userRepository;

    @Mock
    SymmetricEncryption symmetricEncryption;

    @Mock
    private UserMapper userMapper;


    @BeforeEach
    void setUp() throws Exception {
        algoTradeTestDtoFactory = new AlgoTradeTestDtoFactory();
        Mockito.lenient().when(userRepository.findByUserId(any(String.class))).thenReturn(algoTradeTestDtoFactory.getOptionalUserEntity());
        Mockito.lenient().when(symmetricEncryption.encrypt(any(String.class))).thenReturn("EncryptedPassword");
        UserEntity savedEntity = algoTradeTestDtoFactory.getSavedEntity();
        Mockito.lenient().when(userMapper.userRequestToEntity(any(UserRequest.class))).thenReturn(savedEntity);
        Mockito.lenient().when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
    }

    @Test
    void testUserCreationWithInActiveUser() throws Exception {
        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setActive(false);


        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setActive(false);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);

        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getActive() == false);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
    }


    @Test
    void testUserCreationWithActiveUser() throws Exception {
        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        UserEntity savedEntity = algoTradeTestDtoFactory.getSavedEntity();
        Mockito.lenient().when(userMapper.userRequestToEntity(any(UserRequest.class))).thenReturn(savedEntity);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(algoTradeTestDtoFactory.getUserResponse());

        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(userRequest.getActive() == user.getActive());
        assertTrue(userRequest.getIsRealTradingEnabled() == user.getIsRealTradingEnabled());
        assertTrue(user.getUserId() == "User1");
    }


    @Test
    void testUserCreationWithDuplicateUserId() throws Exception {
        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        UserEntity savedEntity = algoTradeTestDtoFactory.getSavedEntity();
        UserEntity existingUser = new UserEntity();
        existingUser.setUserId("User1");
        Mockito.lenient().when(userRepository.findByUserId(any(String.class))).thenReturn(Optional.of(existingUser));
        Mockito.lenient().when(userMapper.userRequestToEntity(any(UserRequest.class))).thenReturn(savedEntity);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(algoTradeTestDtoFactory.getUserResponse());


        AlgotradeException algotradeException = assertThrows(AlgotradeException.class, () -> {
            userService.createUser(userRequest);
        });
        assertTrue(algotradeException.getError() != null);
        assertTrue(algotradeException.getError().getMessage().contains("User id is already exists"));
    }

    @Test
    void testUserCreationWithEnabledStrategies() throws Exception {
        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setEnabledStrategies(List.of("BIGCANDLE"));
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setEnabledStrategies(List.of("BIGCANDLE"));
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);
        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(userRequest.getIsRealTradingEnabled() == user.getIsRealTradingEnabled());
        assertTrue(user.getUserId() == "User1");
        assertTrue(user.getEnabledStrategies().contains("BIGCANDLE"));

    }

    @Test
    void testUserCreationWithoutEnabledStrategies() throws Exception {
        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setEnabledStrategies(List.of("BIGCANDLE"));
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);
        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
        assertTrue(user.getEnabledStrategies() == null);

    }

    @Test
    void testUserCreationWithDayProfitLimit() throws Exception {

        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setEnabledStrategies(List.of("BIGCANDLE"));
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        HashMap<String, BigDecimal> dayProfitLimit = new HashMap<>();
        dayProfitLimit.put("FNO", new BigDecimal(10000));
        userResponse.setDayProfitLimit(dayProfitLimit);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);

        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
        assertTrue(!user.getDayProfitLimit().isEmpty());
        assertTrue(user.getDayProfitLimit().get("FNO").compareTo(new BigDecimal(10000)) == 0);
    }

    @Test
    void testUserCreationWithoutDayProfitLimit() throws Exception {


        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setEnabledStrategies(List.of("BIGCANDLE"));


        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setDayProfitLimit(null);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);

        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
        assertTrue(user.getDayProfitLimit() == null);
    }

    @Test
    void testUserCreationWithDayLossLimit() throws Exception {

        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        HashMap<String, BigDecimal> dayProfitLimit = new HashMap<>();
        dayProfitLimit.put("FNO", new BigDecimal(10000));
        userResponse.setDayProfitLimit(dayProfitLimit);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);

        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setEnabledStrategies(List.of("BIGCANDLE"));
        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
        assertTrue(!user.getDayProfitLimit().isEmpty());
        assertTrue(user.getDayProfitLimit().get("FNO").compareTo(new BigDecimal(10000)) == 0);
    }

    @Test
    void testUserCreationWithoutDayLossLimit() throws Exception {
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setDayLossLimit(null);


        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);

        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setEnabledStrategies(List.of("BIGCANDLE"));
        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
        assertTrue(user.getDayLossLimit() == null);
    }


    @Test
    void testUserCreationWithMinTradesPerDay() throws Exception {

        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        HashMap<String, Integer> minTradesPerDay = new HashMap<>();
        minTradesPerDay.put("FNO", 1);
        userResponse.setMinTradesPerDay(minTradesPerDay);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);

        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setEnabledStrategies(List.of("BIGCANDLE"));
        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
        assertTrue(!user.getMinTradesPerDay().isEmpty());
        assertTrue(user.getMinTradesPerDay().get("FNO") == 1);
    }

    @Test
    void testUserCreationWithoutMinTradesPerDay() throws Exception {
        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setEnabledStrategies(List.of("BIGCANDLE"));
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setMinTradesPerDay(null);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);
        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
        assertTrue(user.getMinTradesPerDay() == null);
    }


    @Test
    void testUserCreationWithMaxTradesPerDay() throws Exception {
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        HashMap<String, Integer> maxTradePerDay = new HashMap<>();
        maxTradePerDay.put("FNO", 3);
        userResponse.setMaxTradesPerDay(maxTradePerDay);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);

        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");

        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
        assertTrue(!user.getMaxTradesPerDay().isEmpty());
        assertTrue(user.getMaxTradesPerDay().get("FNO") == 3);
    }

    @Test
    void testUserCreationWithoutMaxTradesPerDay() throws Exception {
        UserRequest userRequest = algoTradeTestDtoFactory.getUserRequest();
        userRequest.setUserId("User1");
        userRequest.setEnabledStrategies(List.of("BIGCANDLE"));

        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setMaxTradesPerDay(null);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);

        UserResponse user = userService.createUser(userRequest);
        assertNotNull(user);
        assertTrue(user.getIsRealTradingEnabled() == false);
        assertTrue(user.getUserId() == "User1");
        assertTrue(user.getMaxTradesPerDay() == null);

    }

    @Test
    void testGetAllUsersEmpty() {
        Mockito.lenient().when(userRepository.findAll()).thenReturn(new ArrayList<>());
        List<UserResponse> users = userService.getUsers();
        assertNotNull(users);
        assertTrue(users.isEmpty());

    }

    @Test
    void testGetAllUsersNonEmpty() {
        ArrayList<UserEntity> userEntities = new ArrayList<>();
        userEntities.add(algoTradeTestDtoFactory.getSavedEntity());
        userEntities.add(algoTradeTestDtoFactory.getSavedEntity());
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setMinTradesPerDay(null);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);
        Mockito.lenient().when(userRepository.findAll()).thenReturn(userEntities);
        List<UserResponse> users = userService.getUsers();
        assertNotNull(users);
        assertTrue(!users.isEmpty());
        assertTrue(users.size() == 2);
        assertTrue(users.get(0).getIsRealTradingEnabled() == false);
        assertTrue(users.get(0).getUserId() == "User1");

    }


    @Test
    void testDeleteUser() {
        doNothing().when(userRepository).delete(any(UserEntity.class));
        Mockito.lenient().when(userRepository.findById(any(Long.class))).thenReturn(Optional.of(new UserEntity()));
        userService.deleteUser(1l);
        verify(userRepository, times(1)).delete(any(UserEntity.class));
    }

    @Test
    void testDeleteNonExistingUser() {
        Mockito.lenient().when(userRepository.findById(any(Long.class))).thenReturn(Optional.empty());
        AlgotradeException algotradeException = assertThrows(AlgotradeException.class, () -> {
            userService.deleteUser(1l);
        });
        assertTrue(algotradeException.getError() != null);
        assertTrue(algotradeException.getError().getMessage().contains("User is not exists"));
        verify(userRepository, times(0)).delete(any(UserEntity.class));

    }


    @Test
    void testGetUsersbyIds() {
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setMinTradesPerDay(null);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);
        Mockito.lenient().when(userRepository.findAllByUserIdIn(any(List.class))).thenReturn(List.of(algoTradeTestDtoFactory.getSavedEntity()));
        List<UserResponse> userResponses = userService.getUsersbyIds(List.of("User1"));
        assertTrue(!userResponses.isEmpty());
        assertTrue(userResponses.get(0).getUserId().equalsIgnoreCase("User1"));
    }

    @Test
    void testGetUsersbyIdNonExistingUserId() {

        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setMaxTradesPerDay(null);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);
        Mockito.lenient().when(userRepository.findByUserId(any(String.class))).thenReturn(algoTradeTestDtoFactory.getOptionalUserEntity());
        AlgotradeException algotradeException = assertThrows(AlgotradeException.class, () -> {
            userService.getUserById("User1");
        });
        assertTrue(algotradeException.getError() != null);
        assertTrue(algotradeException.getError().getMessage().contains("User is not exists"));
        verify(userRepository, times(0)).delete(any(UserEntity.class));

    }

    @Test
    void testGetUsersbyIdExistingUserId() {
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setMaxTradesPerDay(null);
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId("User1");
        Mockito.lenient().when(userRepository.findByUserId(any(String.class))).thenReturn(Optional.of(userEntity));
        UserResponse user = userService.getUserById("User1");
        assertNotNull(user);
        assertTrue(user.getUserId().equalsIgnoreCase("User1"));
    }

    @Test
    void testGetAllActiveSegmentEnabledUsers() {
        UserEntity userEntity = new UserEntity();
        userEntity.setActive(true);
        userEntity.setUserId("User1");
        Mockito.lenient().when(userRepository.findByActive(any(Boolean.class))).thenReturn(List.of(userEntity));
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setUserId("User1");
        userResponse.setEnabledSegments(List.of("FNO"));
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);
        List<UserResponse> allActiveUsers = userService.getAllActiveSegmentEnabledUsers(Segment.FNO);
        assertTrue(!allActiveUsers.isEmpty());
        assertTrue(allActiveUsers.get(0).getUserId().equalsIgnoreCase("User1"));
    }

    @Disabled
    void testGetStrategyAndSegmentEnabledActiveUserIds() {

    }

    @Disabled
    void testGetUserEntityById() {

    }

    @Test
    void testGetAllActiveUsers() {
        UserEntity userEntity = new UserEntity();
        userEntity.setActive(true);
        userEntity.setUserId("User1");
        Mockito.lenient().when(userRepository.findByActive(any(Boolean.class))).thenReturn(List.of(userEntity));
        UserResponse userResponse = algoTradeTestDtoFactory.getUserResponse();
        userResponse.setUserId("User1");
        Mockito.lenient().when(userMapper.userEntityToResponse(any(UserEntity.class))).thenReturn(userResponse);
        List<UserResponse> allActiveUsers = userService.getAllActiveUsers();
        assertTrue(!allActiveUsers.isEmpty());
        assertTrue(allActiveUsers.get(0).getUserId().equalsIgnoreCase("User1"));
    }
}

