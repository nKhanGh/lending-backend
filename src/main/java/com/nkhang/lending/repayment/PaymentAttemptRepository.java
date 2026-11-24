package com.nkhang.lending.repayment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    boolean existsByInstalmentIdAndStatus(UUID instalmentId, PaymentAttemptStatus status);
    boolean existsByProviderEventId(String providerEventId);
    List<PaymentAttempt> findAllByInstalmentDrawdownIdOrderByRequestedAtDesc(UUID drawdownId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from PaymentAttempt attempt where attempt.id = :id")
    Optional<PaymentAttempt> findByIdForUpdate(@Param("id") UUID id);
}
