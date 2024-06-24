package com.trade.algotrade.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.trade.algotrade.client.angelone.enums.VarietyEnum;
import com.trade.algotrade.client.kotak.dto.OpenPositionsSuccess;
import com.trade.algotrade.client.kotak.response.OpenPositionsResponse;
import com.trade.algotrade.enums.*;
import com.trade.algotrade.request.OrderRequest;
import com.trade.algotrade.response.UserResponse;
import com.trade.algotrade.service.OrderService;
import com.trade.algotrade.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.response.TodaysPositionResponse;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.dto.PositionDto;
import com.trade.algotrade.service.PositionService;

@Service
public class PositionServiceImpl implements PositionService {

    @Autowired
    KotakClient kotakClient;

    @Autowired
    OrderService orderService;

    @Autowired
    UserService userService;

    @Override
    public List<PositionDto> getTodaysPositions(String userId) {
        TodaysPositionResponse todaysOpenPostions = kotakClient.getTodaysOpenPostions(userId);
        if (todaysOpenPostions != null && !CollectionUtils.isEmpty(todaysOpenPostions.getSuccess())) {
            return todaysOpenPostions.getSuccess().stream().map(tp -> {
                PositionDto positionDto = new PositionDto();
                BeanUtils.copyProperties(tp, positionDto);
                return positionDto;
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public List<PositionDto> getTodaysEquityPosition(String userId) {
        List<PositionDto> todaysPositions = getTodaysPositions(userId);
        if (!CollectionUtils.isEmpty(todaysPositions)) {
            return todaysPositions.stream().filter(top -> top.getSegment().equals(Constants.EQUITY)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public List<PositionDto> getTodaysOpenEquityPosition(String userId) {
        List<PositionDto> todaysPosition = getTodaysEquityPosition(userId);
        if (!CollectionUtils.isEmpty(todaysPosition)) {
            // TODO Find the logic to implement open positions
            return todaysPosition.stream().filter(tp -> tp.getRealizedPL().compareTo(BigDecimal.ZERO) == 0 && tp.getBuyOpenQtyLot().compareTo(tp.getSellTradedQtyLot()) != 0).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public List<PositionDto> getOpenPosition(String userId, Segment segment) {

        OpenPositionsResponse allOpenPositions = kotakClient.getAllOpenPositions(userId);

        //TODO need to change mapping
        if (!CollectionUtils.isEmpty(allOpenPositions.getSuccess())) {
            return allOpenPositions.getSuccess().stream().map(tp -> {
                PositionDto positionDto = new PositionDto();
                BeanUtils.copyProperties(tp, positionDto);
                return positionDto;
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public void squareOffOpenPositions(Segment segment) {
        List<UserResponse> allActiveSegmentEnabledUsers = userService.getAllActiveSegmentEnabledUsers(Segment.FNO);
        if (!CollectionUtils.isEmpty(allActiveSegmentEnabledUsers)) {
            allActiveSegmentEnabledUsers.forEach(user -> {
                OpenPositionsResponse allOpenPositions = kotakClient.getAllOpenPositions(user.getUserId());
                if (segment == Segment.FNO) {
                    squreOffFnoPositions(user, allOpenPositions);
                }
                if (segment == Segment.EQ) {
                    squreOffEqPositions(user, allOpenPositions);
                }
            });
        }

    }

    private void squreOffFnoPositions(UserResponse user, OpenPositionsResponse allOpenPositions) {
        if (!CollectionUtils.isEmpty(allOpenPositions.getSuccess())) {
            allOpenPositions.getSuccess().stream().filter(position -> StringUtils.isNoneBlank(position.getSymbol())
                    && position.getSegment().equalsIgnoreCase(Constants.SEGMENT_OPTIONS) && position.getNetTrdQtyLot() != 0).forEach(position -> {
                OrderRequest orderRequest = OrderRequest.builder().build();
                orderRequest.setSegment(Segment.FNO.toString());
                orderRequest.setTriggerType(TriggerType.MANUAL);
                orderRequest.setOptionType(OptionType.valueOf(position.getOptionType()));
                if (position.getNetTrdQtyLot() <= 0) {
                    // Sell poisition needs to revers to BUY
                    orderRequest.setTransactionType(TransactionType.BUY);
                } else {
                    orderRequest.setTransactionType(TransactionType.SELL);
                }
                squareOffPosition(user, position, orderRequest);
            });
        }
    }

    private void squreOffEqPositions(UserResponse user, OpenPositionsResponse allOpenPositions) {
        if (!CollectionUtils.isEmpty(allOpenPositions.getSuccess())) {
            allOpenPositions.getSuccess().stream().filter(position -> StringUtils.isNoneBlank(position.getSymbol())
                    && position.getSegment().equalsIgnoreCase(Segment.EQ.toString())).forEach(position -> {
                OrderRequest orderRequest = OrderRequest.builder().build();
                orderRequest.setSegment(Segment.EQ.toString());
                if (position.getNetTrdQtyLot() <= 0) {
                    orderRequest.setTransactionType(TransactionType.BUY);
                } else {
                    orderRequest.setTransactionType(TransactionType.SELL);
                }
                squareOffPosition(user, position, orderRequest);
            });
        }
    }

    private void squareOffPosition(UserResponse user, OpenPositionsSuccess position, OrderRequest orderRequest) {
        orderRequest.setOrderType(OrderType.MARKET);
        orderRequest.setQuantity(position.getNetTrdQtyLot());
        orderRequest.setPrice(BigDecimal.ZERO);
        orderRequest.setInstrumentToken(position.getInstrumentToken());
        orderRequest.setProduct(com.trade.algotrade.client.angelone.enums.ProductType.INTRADAY);
//        orderRequest.setValidity(ValidityEnum.GFD);
        orderRequest.setVariety(VarietyEnum.NORMAL);
        orderRequest.setUserId(user.getUserId());
        orderRequest.setOrderCategory(OrderCategoryEnum.SQAREOFF);
        orderService.createOrder(orderRequest);
    }
}
