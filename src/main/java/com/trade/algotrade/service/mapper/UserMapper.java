package com.trade.algotrade.service.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.trade.algotrade.enitiy.UserEntity;
import com.trade.algotrade.request.UserRequest;
import com.trade.algotrade.response.UserResponse;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring",injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface UserMapper {


	@Mapping(source = "userRequest.id", target = "id")
	@Mapping(source = "userRequest.userId", target = "userId")
	@Mapping(source = "userRequest.password", target = "password")
	@Mapping(source = "userRequest.accessToken", target = "accessToken")
	@Mapping(source = "userRequest.broker", target = "broker")
	@Mapping(source = "userRequest.appId", target = "appId")
	@Mapping(source = "userRequest.consumerKey", target = "consumerKey")
	@Mapping(source = "userRequest.consumerSecrete", target = "consumerSecrete")
	@Mapping(source = "userRequest.exchange", target = "exchange")
	@Mapping(source = "userRequest.oneTimeToken", target = "oneTimeToken")
	@Mapping(source = "userRequest.sessionToken", target = "sessionToken")
	@Mapping(source = "userRequest.dayProfitLimit", target = "dayProfitLimit")
	@Mapping(source = "userRequest.dayLossLimit", target = "dayLossLimit")
	@Mapping(source = "userRequest.minTradesPerDay", target = "minTradesPerDay")
	@Mapping(source = "userRequest.maxTradesPerDay", target = "maxTradesPerDay")
	@Mapping(source = "userRequest.isRealTradingEnabled", target = "isRealTradingEnabled")
	@Mapping(source = "userRequest.enabledSegments", target = "enabledSegments")
	@Mapping(source = "userRequest.enabledStrategies", target = "enabledStrategies")
	@Mapping(source = "userRequest.active", target = "active")
	@Mapping(source = "userRequest.TOTPEnabled", target = "TOTPEnabled")
	@Mapping(source = "userRequest.totpSecrete", target = "totpSecrete")
	@Mapping(source = "userRequest.feedToken", target = "feedToken")
	@Mapping(source = "userRequest.refreshToken", target = "refreshToken")
	@Mapping(source = "userRequest.systemUser", target = "systemUser")
	UserEntity userRequestToEntity(UserRequest userRequest);

	@Mapping(source = "userEntity.id", target = "id")
	@Mapping(source = "userEntity.userId", target = "userId")
	@Mapping(source = "userEntity.password", target = "password")
	@Mapping(source = "userEntity.accessToken", target = "accessToken")
	@Mapping(source = "userEntity.broker", target = "broker")
	@Mapping(source = "userEntity.appId", target = "appId")
	@Mapping(source = "userEntity.consumerKey", target = "consumerKey")
	@Mapping(source = "userEntity.consumerSecrete", target = "consumerSecrete")
	@Mapping(source = "userEntity.exchange", target = "exchange")
	@Mapping(source = "userEntity.oneTimeToken", target = "oneTimeToken")
	@Mapping(source = "userEntity.sessionToken", target = "sessionToken")
	@Mapping(source = "userEntity.dayProfitLimit", target = "dayProfitLimit")
	@Mapping(source = "userEntity.dayLossLimit", target = "dayLossLimit")
	@Mapping(source = "userEntity.minTradesPerDay", target = "minTradesPerDay")
	@Mapping(source = "userEntity.maxTradesPerDay", target = "maxTradesPerDay")
	@Mapping(source = "userEntity.isRealTradingEnabled", target = "isRealTradingEnabled")
	@Mapping(source = "userEntity.enabledSegments", target = "enabledSegments")
	@Mapping(source = "userEntity.enabledStrategies", target = "enabledStrategies")
	@Mapping(source = "userEntity.active", target = "active")
	@Mapping(source = "userEntity.TOTPEnabled", target = "TOTPEnabled")
	@Mapping(source = "userEntity.totpSecrete", target = "totpSecrete")
	@Mapping(source = "userEntity.feedToken", target = "feedToken")
	@Mapping(source = "userEntity.refreshToken", target = "refreshToken")
	@Mapping(source = "userEntity.systemUser", target = "systemUser")
	@Mapping(source = "userEntity.lastLogin", target = "lastLogin")
	@Mapping(source = "userEntity.telegramChatId", target = "telegramChatId")
	UserResponse userEntityToResponse(UserEntity userEntity);
}
