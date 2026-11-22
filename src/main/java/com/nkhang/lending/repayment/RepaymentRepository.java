package com.nkhang.lending.repayment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepaymentRepository extends JpaRepository<Repayment, UUID> {

    Optional<Repayment> findByInstalmentId(UUID instalmentId);

    @EntityGraph(attributePaths = {"instalment", "instalment.order"})
    @Query("select repayment from Repayment repayment where repayment.id = :id")
    Optional<Repayment> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"instalment", "instalment.order"})
    List<Repayment> findAllByOrderByCreatedAtDesc();
}


