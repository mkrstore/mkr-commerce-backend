package com.mkr.commerce.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mkr.commerce.common.exception.ErrorCode;
import lombok.Getter;

import java.time.Instant;

/**
 * Single envelope for every API response.
 *
 * Success:
 * <pre>
 * {
 *   "success": true,
 *   "message": "Login successful",
 *   "data": { ... },
 *   "timestamp": "2026-05-08T10:00:00Z"
 * }
 * </pre>
 *
 * Error:
 * <pre>
 * {
 *   "success": false,
 *   "message": "Access token has expired.",
 *   "errorCode": "ACCESS_TOKEN_EXPIRED",
 *   "timestamp": "2026-05-08T10:00:00Z"
 * }
 * </pre>
 *
 * {@code data} and {@code errorCode} are omitted from JSON when null.
 * Frontend must switch on {@code errorCode}, never on {@code message}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean   success;
    private final String    message;
    private final String    errorCode;   // null on success
    private final T         data;        // null on error
    private final Instant   timestamp;

    private ApiResponse(boolean success, String message, String errorCode, T data) {
        this.success   = success;
        this.message   = message;
        this.errorCode = errorCode;
        this.data      = data;
        this.timestamp = Instant.now();
    }

    // ── Success factories ─────────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, null, data);
    }

    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    // ── Error factories ───────────────────────────────────────────────────

    public static <T> ApiResponse<T> error(String message, ErrorCode code) {
        return new ApiResponse<>(false, message, code.name(), null);
    }

    /** Fallback — use when no specific code applies (e.g. unexpected exceptions). */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, ErrorCode.INTERNAL_ERROR.name(), null);
    }
}
