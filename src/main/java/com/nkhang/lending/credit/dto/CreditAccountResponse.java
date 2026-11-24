package com.nkhang.lending.credit.dto;

import com.nkhang.lending.credit.CreditAccount;
import java.math.BigDecimal;
import java.util.UUID;

public record CreditAccountResponse(
        UUID accountId,
        BigDecimal totalLimit,
        BigDecimal usedLimit,
        BigDecimal availableLimit,
        String currency,
        String status
) {
    public static CreditAccountResponse from(CreditAccount account) {
        return new CreditAccountResponse(account.getId(), account.getTotalLimit(),
                account.getUsedLimit(), account.getAvailableLimit(), account.getCurrency(),
                account.getStatus().name());
    }
}
