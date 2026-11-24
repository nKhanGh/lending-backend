package com.nkhang.lending.application.dto;

import com.nkhang.lending.application.CreditApplication;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditApplicationResponse(
        UUID applicationId,
        String decision,
        String reasonCode,
        String policyVersion,
        BigDecimal monthlySurplus,
        BigDecimal approvedLimit,
        String currency,
        Instant assessedAt
) {
    public static CreditApplicationResponse from(CreditApplication application) {
        return new CreditApplicationResponse(application.getId(), application.getStatus().name(),
                application.getDecisionReason(), application.getPolicyVersion(),
                application.getSurplus(), application.getApprovedLimit(), "SGD", application.getAssessedAt());
    }
}
