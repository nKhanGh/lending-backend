package com.nkhang.lending.common.idempotency;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByOperationAndIdempotencyKey(
            IdempotencyOperation operation, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IdempotencyRecord> findWithLockByOperationAndIdempotencyKey(
            IdempotencyOperation operation, String idempotencyKey);
}

