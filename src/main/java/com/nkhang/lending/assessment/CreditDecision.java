package com.nkhang.lending.assessment;

import java.math.BigDecimal;

public record CreditDecision(
        DecisionStatus status,
        String reasonCode,
        String policyVersion,
        BigDecimal monthlySurplus,
        BigDecimal approvedLimit
) {
}
