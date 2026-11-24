package com.nkhang.lending.assessment.dto;

import com.nkhang.lending.assessment.CreditProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditProfileResponse(UUID userId, BigDecimal verifiedMonthlyIncome,
                                    int latePayments12m, Instant updatedAt) {
    public static CreditProfileResponse from(CreditProfile profile) {
        return new CreditProfileResponse(profile.getUserId(), profile.getVerifiedMonthlyIncome(),
                profile.getLatePayments12m(), profile.getUpdatedAt());
    }
}
