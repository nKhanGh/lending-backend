package com.nkhang.lending.assessment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CreditAssessmentPolicy {
    public static final String VERSION = "DEMO_V2";

    public CreditDecision assess(BigDecimal income, BigDecimal livingExpenses, BigDecimal debtPayments) {
        return assess(income, livingExpenses, debtPayments, null);
    }

    public CreditDecision assess(BigDecimal income, BigDecimal livingExpenses,
                                 BigDecimal debtPayments, CreditProfile profile) {
        BigDecimal surplus = income.subtract(livingExpenses).subtract(debtPayments);
        if (surplus.compareTo(new BigDecimal("400.00")) < 0) {
            return rejected(surplus, "INSUFFICIENT_SURPLUS");
        }
        if (profile != null && profile.getVerifiedMonthlyIncome() != null
                && income.compareTo(profile.getVerifiedMonthlyIncome().multiply(new BigDecimal("1.10"))) > 0) {
            return rejected(surplus, "INCOME_NOT_VERIFIED");
        }
        if (profile != null && profile.getLatePayments12m() >= 2) {
            return rejected(surplus, "RECENT_LATE_PAYMENTS");
        }
        if (debtPayments.compareTo(income.multiply(new BigDecimal("0.45"))) > 0) {
            return rejected(surplus, "HIGH_DEBT_BURDEN");
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

    private CreditDecision rejected(BigDecimal surplus, String reason) {
        return new CreditDecision(DecisionStatus.REJECTED, reason, VERSION,
                surplus, new BigDecimal("0.00"));
    }
}
