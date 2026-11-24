package com.nkhang.lending.repayment;

import com.nkhang.lending.instalment.Instalment;
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
@Table(name = "payment_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentAttempt {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instalment_id", nullable = false) Instalment instalment;
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) PaymentAttemptStatus status;
    @Column(name = "requested_at", nullable = false) Instant requestedAt;
    @Column(name = "resolved_at") Instant resolvedAt;
    @Column(name = "provider_event_id", length = 120) String providerEventId;

    public static PaymentAttempt pending(UUID userId, Instalment instalment, BigDecimal amount, Instant now) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.id = UUID.randomUUID();
        attempt.userId = userId;
        attempt.instalment = instalment;
        attempt.amount = amount;
        attempt.status = PaymentAttemptStatus.PENDING;
        attempt.requestedAt = now;
        return attempt;
    }

    public void resolve(PaymentAttemptStatus outcome, String eventId, Instant now) {
        if (status != PaymentAttemptStatus.PENDING || outcome == PaymentAttemptStatus.PENDING) {
            throw new IllegalStateException("Invalid payment transition");
        }
        status = outcome;
        providerEventId = eventId;
        resolvedAt = now;
    }
}
