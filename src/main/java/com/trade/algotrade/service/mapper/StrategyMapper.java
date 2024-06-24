package com.trade.algotrade.service.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.trade.algotrade.enitiy.StrategyEnity;
import com.trade.algotrade.request.StrategyRequest;
import com.trade.algotrade.response.StrategyResponse;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface StrategyMapper {

	@Mapping(source = "strategyRequest.id", target = "id")
	@Mapping(source = "strategyRequest.strategyName", target = "strategyName")
	@Mapping(source = "strategyRequest.noOfTargetCompletedOrders", target = "noOfTargetCompletedOrders")
	@Mapping(source = "strategyRequest.noOfSlOrders", target = "noOfSlOrders")
	@Mapping(source = "strategyRequest.successRatio", target = "successRatio")
	StrategyEnity mapStartegyEntity(StrategyRequest strategyRequest);

	@Mapping(source = "strategyEnity.id", target = "id")
	@Mapping(source = "strategyEnity.strategyName", target = "strategyName")
	@Mapping(source = "strategyEnity.noOfTargetCompletedOrders", target = "noOfTargetCompletedOrders")
	@Mapping(source = "strategyEnity.noOfSlOrders", target = "noOfSlOrders")
	@Mapping(source = "strategyEnity.successRatio", target = "successRatio")
	StrategyResponse mapStrategyResponse(StrategyEnity strategyEnity);

}
