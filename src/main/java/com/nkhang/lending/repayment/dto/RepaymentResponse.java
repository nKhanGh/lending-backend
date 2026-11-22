package com.nkhang.lending.repayment.dto;

import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.repayment.Repayment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RepaymentResponse(
        UUID repaymentId,
        UUID instalmentId,
        UUID orderId,
        BigDecimal amount,
        String status,
        Instant paidAt,
        BigDecimal outstandingAmount,
        BigDecimal availableCredit,
        String currency
) {
    public static RepaymentResponse from(Repayment repayment, CreditAccount account) {
        return new RepaymentResponse(
                repayment.getId(),
                repayment.getInstalment().getId(),
                repayment.getInstalment().getOrder().getId(),
                repayment.getAmount(),
                repayment.getInstalment().getStatus().name(),
                repayment.getCreatedAt(),
                account.getOutstandingAmount(),
                account.getAvailableCredit(),
                account.getCurrency()
        );
    }
}

