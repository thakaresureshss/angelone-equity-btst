package com.trade.algotrade.exceptions;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trade.algotrade.utils.DateUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This class will be used to handled business related exceptions.
 * 
 * @author Suresh Thakare
 *
 */

@Setter
@Getter
@AllArgsConstructor
@Builder
@ToString
public class BusinessError implements Serializable {
	private static final long serialVersionUID = 1L;
	private HttpStatus status;
	private String code;
	private String message;
	private String debugMessage;
	private String description;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
	private LocalDateTime timestamp;

	private List<ValidationError> voilations = new ArrayList<>();

	public BusinessError() {
		timestamp = DateUtils.getCurrentDateTimeIst();
	}

	public BusinessError(HttpStatus status) {
		this();
		this.status = status;
	}

	public BusinessError(HttpStatus status, Throwable ex) {
		this();
		this.status = status;
		this.message = "Unexpected error";
		this.debugMessage = ex.getLocalizedMessage();
	}

	public BusinessError(HttpStatus status, String code, Throwable ex) {
		this();
		this.status = status;
		this.code = code;
		this.debugMessage = ex.getLocalizedMessage();
	}

	public BusinessError(HttpStatus status, String code, String message) {
		this();
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public BusinessError(HttpStatus status, String code) {
		this();
		this.status = status;
		this.code = code;
	}

	public BusinessError(HttpStatus status, List<ValidationError> voilations) {
		super();
		this.status = status;
		this.voilations = voilations;
	}

	public BusinessError(HttpStatus status, String message, List<ValidationError> voilations) {
		super();
		this.status = status;
		this.message = message;
		this.voilations = voilations;
	}

	public BusinessError(String message, String description) {
		this();
		this.message = message;
		this.description = description;
	}

}
