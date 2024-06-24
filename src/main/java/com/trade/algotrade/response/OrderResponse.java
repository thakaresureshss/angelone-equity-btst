package com.trade.algotrade.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.trade.algotrade.client.angelone.enums.ProductType;
import com.trade.algotrade.client.kotak.request.SlDetails;
import com.trade.algotrade.client.kotak.dto.CreateOrderDto;

import com.trade.algotrade.enums.*;
import lombok.Data;

@Data
public class OrderResponse {

	private Long orderId;
	private String userId;
	private BigDecimal price;
	private Integer quantity;
	private String orderTimestamp;
	private Long cancelledQuantity;
	private Long disclosedQuantity;
	private CreateOrderDto exchange;
	private String exchangeOrderID;
	private String expiryDate;
	private BigDecimal filledQuantity;
	private String instrumentName;
	private Long instrumentToken;
	private String instrumentType;
	private String isFNO;
	private OptionType optionType;
	private ProductType product;
	private OrderStatus status;
	private String statusInfo;
	private String statusMessage;
	private Integer strikePrice;
	private String tag;
	private TransactionType transactionType;
	private BigDecimal triggerPrice;
	private ValidityEnum validity;
	private com.trade.algotrade.client.angelone.enums.VarietyEnum variety;

	private OrderStatus orderStatus;
	private OrderType orderType;
	private String strategyName;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private List<SlDetails> SlDetailsHistory;
	private String squareOffReason;

	private String segment;
	private Boolean isSlOrderPlaced;
	private OrderCategoryEnum orderCategory;
	private Long originalOrderId;
	private int modificationCount;
	private String tradeId;
	private TriggerType triggerType;


}