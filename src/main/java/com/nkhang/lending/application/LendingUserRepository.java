package com.nkhang.lending.application;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LendingUserRepository extends JpaRepository<LendingUser, UUID> {
    Optional<LendingUser> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from LendingUser user where user.id = :id")
    Optional<LendingUser> findByIdForUpdate(@Param("id") UUID id);
}
