package com.trade.algotrade.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class AlgoValidationException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private String language;
	private BusinessError error;

	public AlgoValidationException(BusinessError error) {
		super();
		this.error = error;
	}

}
