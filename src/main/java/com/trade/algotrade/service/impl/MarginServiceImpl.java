package com.trade.algotrade.service.impl;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.trade.algotrade.client.angelone.AngelOneClient;
import com.trade.algotrade.client.angelone.response.user.GetMarginResponse;
import com.trade.algotrade.client.angelone.response.user.MarginData;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.enitiy.UserEntity;
import com.trade.algotrade.enums.BrokerEnum;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.dto.MarginCoreDto;
import com.trade.algotrade.client.kotak.dto.MarginDto;
import com.trade.algotrade.client.kotak.response.MarginResponse;
import com.trade.algotrade.enitiy.MarginEntity;
import com.trade.algotrade.enums.Segment;
import com.trade.algotrade.repo.MarginRepository;
import com.trade.algotrade.service.MarginService;
import com.trade.algotrade.service.UserService;
import org.springframework.util.ObjectUtils;

@Service
public class MarginServiceImpl implements MarginService {

    private static final Logger logger = LoggerFactory.getLogger(MarginServiceImpl.class);

    @Autowired
    KotakClient kotakClient;

    @Autowired
    UserService userService;

    @Autowired
    MarginRepository marginRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    AngelOneClient angelOneClient;

    @Override
//    @Cacheable(value = "margins", key = "#userId")
    public BigDecimal getMargin(String userId) {
        logger.debug(" ******[ MarginServiceImpl ]:-[ getMargin ] Called  ********");
        Optional<MarginEntity> marginEntityOptional = marginRepository.findByUserId(userId);
        if (marginEntityOptional.isEmpty()) {
            return BigDecimal.ZERO;
        }
        logger.debug(" ******[ MarginServiceImpl ]:-[ getMargin ] Completed  ********");
        Map<String, BigDecimal> margins = marginEntityOptional.get().getMargins();
        if (CollectionUtils.isEmpty(margins)) {
            return BigDecimal.ZERO;
        }
        return margins.get(Segment.EQ.toString());
    }

    @Override
    public MarginData getMarginFromKotak(String userId) {
        logger.debug(" ******[ MarginServiceImpl ]:-[ getMarginFromKotak ] Called for USER ID : = {} ********", userId);
        UserResponse userResponse = userService.getUserById(userId);
        GetMarginResponse margin = angelOneClient.getMargin(userResponse.getSessionToken(),userResponse);
        if (margin != null && margin.getData() != null) {
            MarginData marginData = margin.getData();
            if (marginData != null && marginData.getNet() != null) {
                logger.info(" ******[ MarginServiceImpl ]:-[ getMarginFromKotak ] Completed  for USER ID := {} ********", userId);
                String message = MessageFormat.format("Margin call successful for User : {0} and Margin : {1}", userResponse.getUserId(), marginData.getAvailablecash());
                notificationService.sendTelegramNotification(userResponse.getTelegramChatId(), message);
                return marginData;
            }
        }
        logger.debug(" ******[ MarginServiceImpl ]:-[ getMargin ] Completed for USER ID := {} ********", userId);
        String message = MessageFormat.format("Margin call unsuccessful for User : {0}", userResponse.getUserId());
        notificationService.sendTelegramNotification(userResponse.getTelegramChatId(), message);
        return null;
    }

    @Override
    public void getMarginForAllUser() {
        logger.debug(" Get Margin for user called.");
        List<UserResponse> allActiveUsers = userService.getAllActiveUsersByBroker(BrokerEnum.ANGEL_ONE.toString());
        if (!CollectionUtils.isEmpty(allActiveUsers)) {
            marginRepository.deleteAll();
            allActiveUsers.parallelStream().forEach(user -> {
                try {
                    MarginData margin = getMarginFromKotak(user.getUserId());
                    if (margin != null && !ObjectUtils.isEmpty(margin)) {
                        addMarginInDB(margin, user.getUserId());
                    }
                } catch (Exception e) {
                    logger.error("Exception Occurred while getting or updating margin for the User : {} Exception {}", user.getUserId(), e.getStackTrace());
                }
            });
        }
        logger.debug(" Get Margin for user Completed.");
    }

    @Override
    public void addMarginInDB(MarginData marginData, String userId) {
        logger.debug(" ******[ MarginServiceImpl ]:-[ addMarginInDB ] Called for User {} ********", userId);
        MarginEntity entity = mapMarginDtoToEntity(marginData);
        entity.setUserId(userId);
        try {
            marginRepository.save(entity);
        } catch (Exception e) {
            logger.info(" ******[ MarginServiceImpl ]:-[ addMarginInDB ] for User {}, Exception {} ********", userId, e.getMessage());
        }
        logger.debug(" ******[ MarginServiceImpl ]:-[ addMarginInDB ] Completed User {} ********", userId);
    }

    private MarginEntity mapMarginDtoToEntity(MarginData marginData) {
        MarginEntity entity = new MarginEntity();
        Map<String, BigDecimal> margins = new HashMap<>();
        if (marginData != null) {
            margins.put(Segment.FNO.toString(), new BigDecimal(marginData.getNet()));
            margins.put(Segment.EQ.toString(), new BigDecimal(marginData.getNet()));
        }
        entity.setMargins(margins);
        entity.setCreatedTime(LocalDateTime.now());
        return entity;
    }

    @Override
    @CacheEvict(value = "margins", key = "#userId")
    public void updateMarginInDB(String userId, BigDecimal updatedMargin) {
        logger.debug("updateMargin called for USER := {}, Updated Margin := {} ", userId, updatedMargin);
        Optional<MarginEntity> marginEntityOptional = marginRepository.findByUserId(userId);
        if (marginEntityOptional.isPresent()) {
            MarginEntity marginEntity = marginEntityOptional.get();
            logger.info("Updating Margin for USER := {}, Updated Margin := {} , Old Margin Was :={}", userId, updatedMargin, marginEntity.getMargins().get(Segment.EQ.toString()));
            Map<String, BigDecimal> margins = marginEntity.getMargins();
            if (!CollectionUtils.isEmpty(margins)) {
                margins.put(Segment.EQ.toString(), updatedMargin);
            } else {
                margins = new HashMap<>();
                margins.put(Segment.EQ.toString(), updatedMargin);
            }
            marginEntity.setMargins(margins);
            marginEntity.setModifiedTime(LocalDateTime.now());
        }

    }

}