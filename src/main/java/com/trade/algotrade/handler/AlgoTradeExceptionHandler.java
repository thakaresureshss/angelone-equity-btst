package com.trade.algotrade.handler;

import com.trade.algotrade.dto.ErrorDetails;
import com.trade.algotrade.exceptions.*;
import com.trade.algotrade.scheduler.WebsocketConnectonScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@Validated
@ControllerAdvice
public class AlgoTradeExceptionHandler extends ResponseEntityExceptionHandler {
    @Autowired
    ApplicationContext context;
    private final Logger logger = LoggerFactory.getLogger(WebsocketConnectonScheduler.class);

    @ExceptionHandler(AlgotradeException.class)
    public ResponseEntity<BusinessError> handleAlgotradeException(AlgotradeException ex, WebRequest request) {
        logger.error("Handling AlgotradeException '{}'" + ex.getError());
        BusinessError error = ex.getError();
        return new ResponseEntity<BusinessError>(error, error.getStatus());
    }

    @ExceptionHandler(KotakTradeApiException.class)
    public ResponseEntity<BusinessError> handleKitakTradeApiException(KotakTradeApiException ex, WebRequest request) {
        logger.error("Handling KotakTradeApiException '{}'" + ex.getError());
        BusinessError error = ex.getError();
        return new ResponseEntity<BusinessError>(error, error.getStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errorList = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage()).collect(Collectors.toList());
        ErrorDetails errorDetails = new ErrorDetails(HttpStatus.BAD_REQUEST, errorList);
        return handleExceptionInternal(ex, errorDetails, headers, errorDetails.getStatus(), request);
    }

    @ExceptionHandler(KotakValidationException.class)
    public ResponseEntity<BusinessError> handleKotakValidationException(KotakValidationException ex,
                                                                        WebRequest request) {
        ex.printStackTrace();
        logger.error("Handling Exception '{}'" + ex);
        BusinessError error = ex.getError();
        return new ResponseEntity<BusinessError>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BusinessError> handleGlobalException(Exception ex, WebRequest request) {
        ex.printStackTrace();
        logger.error("Handling Exception '{}'" + ex.getMessage());
        BusinessError exception = new BusinessError(ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<BusinessError>(exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AlgoValidationException.class)
    public ResponseEntity<BusinessError> handleKotakValidationException(AlgoValidationException ex,
                                                                        WebRequest request) {
        ex.printStackTrace();
        logger.error("Handling AlgoValidationException '{}'" + ex);
        BusinessError error = ex.getError();
        return new ResponseEntity<BusinessError>(error, HttpStatus.BAD_REQUEST);
    }
}
