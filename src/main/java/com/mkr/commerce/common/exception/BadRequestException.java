package com.mkr.commerce.common.exception;

import org.springframework.http.HttpStatus;

/** 400 — invalid input or business rule violation. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message, ErrorCode code) {
        super(HttpStatus.BAD_REQUEST, message, code);
    }

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message, ErrorCode.VALIDATION_ERROR);
    }
}
