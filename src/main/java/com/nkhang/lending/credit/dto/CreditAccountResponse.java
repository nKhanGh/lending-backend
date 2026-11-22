package com.nkhang.lending.credit.dto;

import com.nkhang.lending.credit.CreditAccount;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditAccountResponse(
        UUID accountId,
        UUID userId,
        BigDecimal totalLimit,
        BigDecimal outstandingAmount,
        BigDecimal reservedAmount,
        BigDecimal availableCredit,
        String currency
) {
    public static CreditAccountResponse from(CreditAccount account) {
        return new CreditAccountResponse(
                account.getId(),
                account.getUserId(),
                account.getTotalLimit(),
                account.getOutstandingAmount(),
                account.getReservedAmount(),
                account.getAvailableCredit(),
                account.getCurrency()
        );
    }
}

