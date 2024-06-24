package com.trade.algotrade.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AlgotradeException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	private BusinessError error;
}
