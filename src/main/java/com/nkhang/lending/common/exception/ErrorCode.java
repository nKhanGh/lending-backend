package com.nkhang.lending.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(40000, "Invalid request", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(40400, "Resource not found", HttpStatus.NOT_FOUND),
    APPLICATION_ALREADY_APPROVED(40901, "Credit account already exists", HttpStatus.CONFLICT),
    INSUFFICIENT_LIMIT(40902, "Insufficient available credit", HttpStatus.CONFLICT),
    DRAWDOWN_NOT_ACTIVE(40903, "Drawdown is not active", HttpStatus.CONFLICT),
    INSTALMENT_ALREADY_PAID(40904, "Instalment already paid", HttpStatus.CONFLICT),
    INSTALMENT_OUT_OF_ORDER(40905, "Pay the earliest unpaid instalment first", HttpStatus.CONFLICT),
    IDEMPOTENCY_KEY_REUSED(40906, "Idempotency key used for a different request", HttpStatus.CONFLICT),
    REQUEST_IN_PROGRESS(40907, "Request is in progress", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(50000, "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
