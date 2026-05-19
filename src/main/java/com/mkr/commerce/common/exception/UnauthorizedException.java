package com.mkr.commerce.common.exception;

import org.springframework.http.HttpStatus;

/** 401 — missing, invalid, or expired credentials / token. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message, ErrorCode code) {
        super(HttpStatus.UNAUTHORIZED, message, code);
    }
}
