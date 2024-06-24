package com.trade.algotrade.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorDetails {
	private HttpStatus status;
	private List<String> errors;

	public ErrorDetails(HttpStatus status, List<String> errors) {
		super();
		this.status = status;
		this.errors = errors;
	}

	public ErrorDetails(HttpStatus status, String error) {
		super();
		this.status = status;
		errors = Collections.singletonList(error);
	}
}