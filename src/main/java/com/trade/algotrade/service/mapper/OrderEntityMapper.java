package com.trade.algotrade.service.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.trade.algotrade.enitiy.ClosedOrderEntity;
import com.trade.algotrade.enitiy.OpenOrderEntity;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface OrderEntityMapper {

	@Mapping(source = "openOrderEntity.userId", target = "userId")
	@Mapping(source = "openOrderEntity.transactionType", target = "transactionType")
	@Mapping(source = "openOrderEntity.quantity", target = "quantity")
	@Mapping(source = "openOrderEntity.price", target = "price")
	@Mapping(source = "openOrderEntity.validity", target = "validity")
	@Mapping(source = "openOrderEntity.variety", target = "variety")
	@Mapping(source = "openOrderEntity.slDetails", target = "slDetails")
	@Mapping(source = "openOrderEntity.targetDetails", target = "targetDetails")
	@Mapping(source = "openOrderEntity.instrumentToken", target = "instrumentToken")
	@Mapping(source = "openOrderEntity.orderId", target = "orderId")
	@Mapping(source = "openOrderEntity.optionType", target = "optionType")
	@Mapping(source = "openOrderEntity.strikePrice", target = "strikePrice")
	@Mapping(source = "openOrderEntity.orderStatus", target = "orderStatus")
	@Mapping(source = "openOrderEntity.orderType", target = "orderType")
	ClosedOrderEntity openToHistoryOrderEntity(OpenOrderEntity openOrderEntity);

}
