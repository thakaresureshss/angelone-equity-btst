package com.trade.algotrade.client.kotak.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class MarginCoreDto {
	private BigDecimal additionalOptionBrokerage;
	private BigDecimal availableCashBalance;
	private BigDecimal clientGroupLimit;
	private BigDecimal debtorFlag;
	private BigDecimal dtv;
	private BigDecimal dtvBTSTSell;
	private BigDecimal initialMargin;
	private BigDecimal marginAvailable;
	private BigDecimal marginUtilised;
	private BigDecimal mfLien;
	private BigDecimal mtm;
	private BigDecimal nriPinsBalance;
	private BigDecimal optionPremium;
	private BigDecimal realizedPL;
	private BigDecimal securityMargin;
	private BigDecimal totalMargin;
}
