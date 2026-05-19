package com.mkr.commerce.common.exception;

import org.springframework.http.HttpStatus;

/** 403 — authenticated but not authorised for this resource. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message, ErrorCode.FORBIDDEN);
    }
}
