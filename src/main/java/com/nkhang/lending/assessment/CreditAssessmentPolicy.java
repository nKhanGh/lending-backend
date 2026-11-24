package com.nkhang.lending.assessment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CreditAssessmentPolicy {
    public static final String VERSION = "DEMO_V1";

    public CreditDecision assess(BigDecimal income, BigDecimal livingExpenses, BigDecimal debtPayments) {
        BigDecimal surplus = income.subtract(livingExpenses).subtract(debtPayments);
        if (surplus.compareTo(new BigDecimal("400.00")) < 0) {
            return new CreditDecision(DecisionStatus.REJECTED, "INSUFFICIENT_SURPLUS", VERSION,
                    surplus, new BigDecimal("0.00"));
        }
        if (surplus.compareTo(new BigDecimal("800.00")) < 0) {
            return approved(surplus, "APPROVED_TIER_500", "500.00");
        }
        if (surplus.compareTo(new BigDecimal("1500.00")) < 0) {
            return approved(surplus, "APPROVED_TIER_1000", "1000.00");
        }
        return approved(surplus, "APPROVED_TIER_2000", "2000.00");
    }

    private CreditDecision approved(BigDecimal surplus, String reason, String limit) {
        return new CreditDecision(DecisionStatus.APPROVED, reason, VERSION,
                surplus, new BigDecimal(limit));
    }
}
