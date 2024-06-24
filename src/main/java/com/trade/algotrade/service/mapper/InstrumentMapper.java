package com.trade.algotrade.service.mapper;

import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.response.InstrumentResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface InstrumentMapper {
   @Mapping(source = "angelOneInstrumentMasterEntity.instrumentToken", target = "instrumentToken")
   @Mapping(source = "angelOneInstrumentMasterEntity.instrumentName", target = "instrumentName")
   @Mapping(source = "angelOneInstrumentMasterEntity.name", target = "name")
   @Mapping(source = "angelOneInstrumentMasterEntity.lastPrice", target = "lastPrice")
   @Mapping(source = "angelOneInstrumentMasterEntity.expiry", target = "expiry")
   @Mapping(source = "angelOneInstrumentMasterEntity.strike", target = "strike")
   @Mapping(source = "angelOneInstrumentMasterEntity.tickSize", target = "tickSize")
   @Mapping(source = "angelOneInstrumentMasterEntity.lotSize", target = "lotSize")
   @Mapping(source = "angelOneInstrumentMasterEntity.instrumentType", target = "instrumentType")
   @Mapping(source = "angelOneInstrumentMasterEntity.segment", target = "segment")
   @Mapping(source = "angelOneInstrumentMasterEntity.exchange", target = "exchange")
   @Mapping(source = "angelOneInstrumentMasterEntity.multiplier", target = "multiplier")
   @Mapping(source = "angelOneInstrumentMasterEntity.exchangeToken", target = "exchangeToken")
   @Mapping(source = "angelOneInstrumentMasterEntity.optionType", target = "optionType")
   @Mapping(source = "angelOneInstrumentMasterEntity.tradingSymbol", target = "tradingSymbol")
   InstrumentResponse entityToInstrumentResponse(AngelOneInstrumentMasterEntity angelOneInstrumentMasterEntity);
}
