package com.nkhang.lending.application;

import com.nkhang.lending.assessment.CreditDecision;
import com.nkhang.lending.assessment.DecisionStatus;
import com.nkhang.lending.application.dto.CreditApplicationRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "credit_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditApplication {
    @Id
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    DemoUser user;

    @Column(name = "monthly_net_income", nullable = false, precision = 19, scale = 2)
    BigDecimal monthlyNetIncome;

    @Column(name = "living_expenses", nullable = false, precision = 19, scale = 2)
    BigDecimal livingExpenses;

    @Column(name = "debt_payments", nullable = false, precision = 19, scale = 2)
    BigDecimal debtPayments;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal surplus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    DecisionStatus status;

    @Column(name = "decision_reason", nullable = false, length = 40)
    String decisionReason;

    @Column(name = "policy_version", nullable = false, length = 20)
    String policyVersion;

    @Column(name = "approved_limit", nullable = false, precision = 19, scale = 2)
    BigDecimal approvedLimit;

    @Column(name = "assessed_at", nullable = false)
    Instant assessedAt;

    public static CreditApplication assessed(DemoUser user, CreditApplicationRequest request,
                                             CreditDecision decision, Instant now) {
        CreditApplication application = new CreditApplication();
        application.id = UUID.randomUUID();
        application.user = user;
        application.monthlyNetIncome = request.monthlyNetIncome();
        application.livingExpenses = request.monthlyLivingExpenses();
        application.debtPayments = request.monthlyDebtPayments();
        application.surplus = decision.monthlySurplus();
        application.status = decision.status();
        application.decisionReason = decision.reasonCode();
        application.policyVersion = decision.policyVersion();
        application.approvedLimit = decision.approvedLimit();
        application.assessedAt = now;
        return application;
    }
}
