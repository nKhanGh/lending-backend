package com.nkhang.lending.drawdown.dto;

import com.nkhang.lending.drawdown.CreditDrawdown;
import com.nkhang.lending.instalment.Instalment;
import com.nkhang.lending.instalment.dto.InstalmentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DrawdownDetailResponse(
        UUID drawdownId,
        BigDecimal originalAmount,
        BigDecimal outstandingPrincipal,
        BigDecimal availableLimit,
        String currency,
        String status,
        Instant createdAt,
        List<InstalmentResponse> instalments
) {
    public static DrawdownDetailResponse from(CreditDrawdown drawdown, List<Instalment> instalments, LocalDate today) {
        return new DrawdownDetailResponse(drawdown.getId(), drawdown.getOriginalAmount(),
                drawdown.getOutstandingPrincipal(), drawdown.getAccount().getAvailableLimit(),
                drawdown.getAccount().getCurrency(), drawdown.getStatus().name(),
                drawdown.getCreatedAt(), instalments.stream().map(item -> InstalmentResponse.from(item, today)).toList());
    }
}
