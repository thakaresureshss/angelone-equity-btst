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
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AngelOneValidationException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private String language;
	private BusinessError error;

	public AngelOneValidationException(BusinessError error) {
		super();
		this.error = error;
	}

}
