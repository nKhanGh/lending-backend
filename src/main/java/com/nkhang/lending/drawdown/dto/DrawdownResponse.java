package com.nkhang.lending.drawdown.dto;

import com.nkhang.lending.drawdown.CreditDrawdown;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DrawdownResponse(UUID drawdownId, BigDecimal originalAmount, BigDecimal outstandingPrincipal,
                               CreditDrawdown.Status status, Instant createdAt, int instalments,
                               BigDecimal availableLimit) {
    public static DrawdownResponse from(CreditDrawdown drawdown, BigDecimal availableLimit) {
        return new DrawdownResponse(drawdown.getId(), drawdown.getOriginalAmount(),
                drawdown.getOutstandingPrincipal(), drawdown.getStatus(), drawdown.getCreatedAt(),
                4, availableLimit);
    }

    public static DrawdownResponse fromReplay(CreditDrawdown drawdown, BigDecimal currentAvailableLimit) {
        return new DrawdownResponse(drawdown.getId(), drawdown.getOriginalAmount(),
                drawdown.getOriginalAmount(), CreditDrawdown.Status.ACTIVE, drawdown.getCreatedAt(),
                4, drawdown.getAvailableLimitAfter() == null
                ? currentAvailableLimit : drawdown.getAvailableLimitAfter());
    }
}
