package com.trade.algotrade.client.kotak.dto;

import java.util.List;

import lombok.Data;

@Data
public class MarginSuccessDto {
	private List<MarginDto> derivatives;
}
