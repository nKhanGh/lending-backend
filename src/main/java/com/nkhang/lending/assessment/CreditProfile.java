package com.nkhang.lending.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "credit_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditProfile {
    @Id
    @Column(name = "user_id")
    UUID userId;
    @Column(name = "verified_monthly_income", precision = 19, scale = 2)
    BigDecimal verifiedMonthlyIncome;
    @Column(name = "late_payments_12m", nullable = false)
    int latePayments12m;
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    public static CreditProfile of(UUID userId, BigDecimal income, int latePayments, Instant now) {
        CreditProfile profile = new CreditProfile();
        profile.userId = userId;
        profile.update(income, latePayments, now);
        return profile;
    }

    public void update(BigDecimal income, int latePayments, Instant now) {
        verifiedMonthlyIncome = income;
        latePayments12m = latePayments;
        updatedAt = now;
    }
}
