package com.nkhang.lending.repayment.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SettlementResponse(
        UUID orderId,
        List<UUID> repaymentIds,
        int instalmentsPaid,
        BigDecimal amount,
        BigDecimal outstandingAmount,
        BigDecimal availableCredit,
        String currency
) {
}
