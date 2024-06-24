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
public class KotakTradeDateException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private String language;
	private BusinessError error;

	public KotakTradeDateException(BusinessError error) {
		super();
		this.error = error;
	}

}
