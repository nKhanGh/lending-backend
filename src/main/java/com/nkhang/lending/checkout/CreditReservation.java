package com.nkhang.lending.checkout;

import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "credit_reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditReservation {

    @Id
    UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    Order order;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    public static CreditReservation active(Order order, BigDecimal amount, Instant now, Instant expiresAt) {
        CreditReservation reservation = new CreditReservation();
        reservation.id = UUID.randomUUID();
        reservation.order = order;
        reservation.amount = amount;
        reservation.status = ReservationStatus.ACTIVE;
        reservation.expiresAt = expiresAt;
        reservation.createdAt = now;
        reservation.updatedAt = now;
        return reservation;
    }

    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(instant);
    }

    public void capture(Instant now) {
        requireActive();
        status = ReservationStatus.CAPTURED;
        updatedAt = now;
    }

    public void release(Instant now) {
        requireActive();
        status = ReservationStatus.RELEASED;
        updatedAt = now;
    }

    public void expire(Instant now) {
        requireActive();
        status = ReservationStatus.EXPIRED;
        updatedAt = now;
    }

    private void requireActive() {
        if (status != ReservationStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_CHECKOUT_STATE);
        }
    }
}

