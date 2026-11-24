package com.nkhang.lending.repayment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RepaymentResponse(
        UUID repaymentId,
        UUID instalmentId,
        UUID drawdownId,
        BigDecimal paidAmount,
        BigDecimal remainingDrawdownPrincipal,
        BigDecimal usedLimit,
        BigDecimal availableLimit,
        String instalmentStatus,
        String drawdownStatus,
        Instant paidAt
) {
}
