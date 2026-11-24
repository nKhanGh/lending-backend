package com.nkhang.lending.drawdown;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditDrawdownRepository extends JpaRepository<CreditDrawdown, UUID> {
    @Query("select drawdown from CreditDrawdown drawdown where drawdown.account.user.id = :userId order by drawdown.createdAt desc")
    List<CreditDrawdown> findAllForUser(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select drawdown from CreditDrawdown drawdown where drawdown.id = :id")
    Optional<CreditDrawdown> findByIdForUpdate(@Param("id") UUID id);
}
