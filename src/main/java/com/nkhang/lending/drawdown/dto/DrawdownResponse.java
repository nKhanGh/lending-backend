package com.nkhang.lending.drawdown.dto;

import com.nkhang.lending.drawdown.CreditDrawdown;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DrawdownResponse(
        UUID drawdownId,
        BigDecimal originalAmount,
        BigDecimal outstandingPrincipal,
        BigDecimal availableLimit,
        String currency,
        String status,
        Instant createdAt
) {
    public static DrawdownResponse from(CreditDrawdown drawdown) {
        return new DrawdownResponse(drawdown.getId(), drawdown.getOriginalAmount(),
                drawdown.getOutstandingPrincipal(), drawdown.getAccount().getAvailableLimit(),
                drawdown.getAccount().getCurrency(), drawdown.getStatus().name(), drawdown.getCreatedAt());
    }
}
