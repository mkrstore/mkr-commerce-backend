package com.mkr.commerce.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Root of all application exceptions.
 * Every subclass carries an HTTP status AND a machine-readable ErrorCode.
 * GlobalExceptionHandler maps these to ApiResponse.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus  status;
    private final ErrorCode   errorCode;

    public ApiException(HttpStatus status, String message, ErrorCode errorCode) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
    }
}
