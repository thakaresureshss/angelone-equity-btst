package com.trade.algotrade.exceptions;

import lombok.*;

/**
 * This class will be used to handled business related exceptions.
 * 
 * @author Suresh Thakare
 *
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

@ToString
public class KotakTradeApiException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private String language;
	private BusinessError error;

	public KotakTradeApiException(BusinessError error) {
		super();
		this.error = error;
	}

}
