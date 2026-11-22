package com.nkhang.lending.instalment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstalmentRepository extends JpaRepository<Instalment, UUID> {

    List<Instalment> findAllByOrderIdOrderBySequenceNoAsc(UUID orderId);

    long countByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select instalment from Instalment instalment where instalment.order.id = :orderId order by instalment.sequenceNo")
    List<Instalment> findAllByOrderIdForUpdate(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "order")
    @Query("select instalment from Instalment instalment where instalment.id = :id")
    Optional<Instalment> findByIdForUpdate(@Param("id") UUID id);
}

