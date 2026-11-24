package com.nkhang.lending.instalment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstalmentRepository extends JpaRepository<Instalment, UUID> {
    List<Instalment> findAllByDrawdownIdOrderBySequenceNoAsc(UUID drawdownId);

    Optional<Instalment> findFirstByDrawdownIdAndStatusOrderBySequenceNoAsc(
            UUID drawdownId, InstalmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select instalment from Instalment instalment where instalment.id = :id")
    Optional<Instalment> findByIdForUpdate(@Param("id") UUID id);
}
