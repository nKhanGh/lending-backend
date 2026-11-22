package com.nkhang.lending.repayment.dto;

import com.nkhang.lending.repayment.Repayment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RepaymentHistoryResponse(
        UUID repaymentId,
        UUID instalmentId,
        UUID orderId,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {
    public static RepaymentHistoryResponse from(Repayment repayment) {
        return new RepaymentHistoryResponse(
                repayment.getId(),
                repayment.getInstalment().getId(),
                repayment.getInstalment().getOrder().getId(),
                repayment.getAmount(),
                repayment.getInstalment().getOrder().getCurrency(),
                repayment.getCreatedAt()
        );
    }
}

