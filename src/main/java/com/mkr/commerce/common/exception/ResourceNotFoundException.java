package com.mkr.commerce.common.exception;

import org.springframework.http.HttpStatus;

/** 404 — requested entity does not exist. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND,
              resource + " not found with id: " + id,
              ErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message, ErrorCode.RESOURCE_NOT_FOUND);
    }
}
