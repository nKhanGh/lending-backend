package com.nkhang.lending.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(40001, "Invalid request", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(40401, "Resource not found", HttpStatus.NOT_FOUND),
    INSUFFICIENT_CREDIT(42201, "Insufficient available credit", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_CHECKOUT_STATE(40901, "Checkout is not in a valid state for this operation", HttpStatus.CONFLICT),
    RESERVATION_EXPIRED(40902, "Credit reservation has expired", HttpStatus.CONFLICT),
    INSTALMENT_ALREADY_PAID(40903, "Instalment has already been paid", HttpStatus.CONFLICT),
    ORDER_ALREADY_SETTLED(40906, "Order has no unpaid instalments", HttpStatus.CONFLICT),
    IDEMPOTENCY_KEY_REUSED(40904, "Idempotency key was already used with a different request", HttpStatus.CONFLICT),
    REQUEST_IN_PROGRESS(40905, "An identical request is currently in progress", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(50000, "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}

