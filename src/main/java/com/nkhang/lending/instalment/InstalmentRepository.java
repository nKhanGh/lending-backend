package com.nkhang.lending.instalment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface InstalmentRepository extends JpaRepository<Instalment, UUID> {
    List<Instalment> findAllByDrawdownIdOrderBySequenceNoAsc(UUID drawdownId);

    Optional<Instalment> findFirstByDrawdownIdAndStatusInOrderBySequenceNoAsc(
            UUID drawdownId, Collection<InstalmentStatus> statuses);

    @Modifying
    @Query(value = "update instalments set status = 'OVERDUE' where status = 'UNPAID' and due_date < :today",
            nativeQuery = true)
    int markOverdue(@Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select instalment from Instalment instalment where instalment.id = :id")
    Optional<Instalment> findByIdForUpdate(@Param("id") UUID id);
}
