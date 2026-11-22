package com.nkhang.lending.checkout;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditReservationRepository extends JpaRepository<CreditReservation, UUID> {

    Optional<CreditReservation> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from CreditReservation reservation where reservation.order.id = :orderId")
    Optional<CreditReservation> findByOrderIdForUpdate(@Param("orderId") UUID orderId);

    List<CreditReservation> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            ReservationStatus status, Instant expiresAt);
}
