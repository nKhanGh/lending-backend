package com.nkhang.lending;

import com.nkhang.lending.assessment.CreditAssessmentPolicy;
import com.nkhang.lending.assessment.DecisionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CreditAssessmentPolicyTest {
    private final CreditAssessmentPolicy policy = new CreditAssessmentPolicy();

    @Test
    void exactTierBoundariesAreDeterministic() {
        assertThat(policy.assess(amount("399.99"), amount("0.00"), amount("0.00")).status())
                .isEqualTo(DecisionStatus.REJECTED);
        assertThat(policy.assess(amount("400.00"), amount("0.00"), amount("0.00")).approvedLimit())
                .isEqualByComparingTo("500.00");
        assertThat(policy.assess(amount("800.00"), amount("0.00"), amount("0.00")).approvedLimit())
                .isEqualByComparingTo("1000.00");
        assertThat(policy.assess(amount("1500.00"), amount("0.00"), amount("0.00")).approvedLimit())
                .isEqualByComparingTo("2000.00");
        assertThat(policy.assess(amount("3000.00"), amount("1400.00"), amount("500.00"))
                .reasonCode()).isEqualTo("APPROVED_TIER_1000");
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
