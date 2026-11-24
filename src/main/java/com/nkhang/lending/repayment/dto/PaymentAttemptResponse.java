package com.nkhang.lending.repayment.dto;

import com.nkhang.lending.repayment.PaymentAttempt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptResponse(UUID paymentAttemptId, UUID instalmentId, UUID drawdownId,
                                     BigDecimal amount, String status, Instant requestedAt, Instant resolvedAt) {
    public static PaymentAttemptResponse from(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(attempt.getId(), attempt.getInstalment().getId(),
                attempt.getInstalment().getDrawdown().getId(), attempt.getAmount(),
                attempt.getStatus().name(), attempt.getRequestedAt(), attempt.getResolvedAt());
    }
}
