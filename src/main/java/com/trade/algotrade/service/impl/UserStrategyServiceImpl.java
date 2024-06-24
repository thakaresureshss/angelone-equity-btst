package com.trade.algotrade.service.impl;

import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.dto.TradeOrderConfigDto;
import com.trade.algotrade.enitiy.UserEntity;
import com.trade.algotrade.enitiy.UserStrategyEntity;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.repo.UserStrategyRepository;
import com.trade.algotrade.request.OrderConfigUpdateRequest;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.service.UserStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

@Service
public class UserStrategyServiceImpl implements UserStrategyService {

    @Autowired
    UserStrategyRepository userStrategyRepository;

    @Autowired
    UserService userService;

    @Override
    public List<UserStrategyEntity> findStrategyByUserId(String userId) {
        return userStrategyRepository.findByUserId(userId);
    }

    @Cacheable(value = "userStrategies", key = "{#userId, #strategyName}")
    @Override
    public UserStrategyEntity findStrategyByUserIdAndStrategy(String userId, String strategyName) {
        Optional<UserStrategyEntity> findByUserIdAndStrategyName = userStrategyRepository
                .findByUserIdAndStrategyName(userId, strategyName);
        return findByUserIdAndStrategyName.orElse(null);
    }

    @Override
    public void updateOrderConfig(String strategyName, OrderConfigUpdateRequest orderConfigUpdateRequest) {
        List<UserStrategyEntity> userStrategies = userStrategyRepository.findByStrategyName(strategyName);
        if (CollectionUtils.isEmpty(userStrategies)) {
            throw new AlgotradeException(ErrorCodeConstants.STRATEGY_NOT_FOUND);
        }
        List<TradeOrderConfigDto> tradeQuantities = orderConfigUpdateRequest.getTradeQuantities();
        if (CollectionUtils.isEmpty(tradeQuantities)) {
            throw new AlgotradeException(ErrorCodeConstants.TRADE_QUANTITY_NOTHING_TO_UPDATE);
        }
        Set<UserStrategyEntity> userStrategyEntitySet = new HashSet<>();
        for (TradeOrderConfigDto tradeOrderConfigDto : tradeQuantities) {
            updateUserConfig(tradeOrderConfigDto);
            updateUserStrategyConfig(userStrategies, userStrategyEntitySet, tradeOrderConfigDto);
        }
        userStrategyRepository.saveAll(userStrategyEntitySet);
    }

    @Override
    @CacheEvict(value = "userStrategies")
    public UserStrategyEntity saveUserStrategy(UserStrategyEntity userStrategyEntity) {
        return userStrategyRepository.save(userStrategyEntity);
    }

    private static void updateUserStrategyConfig(List<UserStrategyEntity> userStrategies, Set<UserStrategyEntity> userStrategyEntitySet, TradeOrderConfigDto tradeOrderConfigDto) {
        Optional<UserStrategyEntity> userStrategyEntityOptional = userStrategies.stream().filter(userStrategy -> userStrategy.getUserId().equalsIgnoreCase(tradeOrderConfigDto.getUserId())).findFirst();
        if (userStrategyEntityOptional.isPresent()) {
            UserStrategyEntity userStrategyEntity = userStrategyEntityOptional.get();
            userStrategyEntity.setQuantity(tradeOrderConfigDto.getTradeQuantity());
            userStrategyEntitySet.add(userStrategyEntity);
        }
    }

    private void updateUserConfig(TradeOrderConfigDto tradeOrderConfigDto) {
        UserEntity dbUser = userService.getUserByUserId(tradeOrderConfigDto.getUserId());
        Map<String, Integer> maxTradesPerDay = dbUser.getMaxTradesPerDay();
        if (tradeOrderConfigDto.getNoOfTradePerDay() != null) {
            maxTradesPerDay.put(Segment.FNO.toString(), tradeOrderConfigDto.getNoOfTradePerDay());
            dbUser.setMaxTradesPerDay(maxTradesPerDay);
        }


        Map<String, BigDecimal> dayLossLimitMap = dbUser.getDayLossLimit();
        if (tradeOrderConfigDto.getMaxLossPerDay() != null) {
            dayLossLimitMap.put(Segment.FNO.toString(), tradeOrderConfigDto.getMaxLossPerDay());
            dbUser.setDayLossLimit(dayLossLimitMap);
        } else if (tradeOrderConfigDto.getTradeQuantity() != null) {
            dayLossLimitMap.put(Segment.FNO.toString(), new BigDecimal(35).multiply(new BigDecimal(tradeOrderConfigDto.getTradeQuantity())));
            dbUser.setDayLossLimit(dayLossLimitMap);
        }

        Map<String, BigDecimal> dayProfitLimitMap = dbUser.getDayProfitLimit();
        if (tradeOrderConfigDto.getMaxProfitPerDay() != null) {
            dayProfitLimitMap.put(Segment.FNO.toString(), tradeOrderConfigDto.getMaxProfitPerDay());
            dbUser.setDayProfitLimit(dayProfitLimitMap);
        } else {
            dayProfitLimitMap.put(Segment.FNO.toString(), dayLossLimitMap.get(Segment.FNO.toString()).multiply(new BigDecimal(2.5)));
            dbUser.setDayProfitLimit(dayProfitLimitMap);
        }
        userService.updateUserEntity(dbUser);
    }
}
