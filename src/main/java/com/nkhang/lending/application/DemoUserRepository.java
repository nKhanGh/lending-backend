package com.nkhang.lending.application;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DemoUserRepository extends JpaRepository<DemoUser, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from DemoUser user where user.id = :id")
    Optional<DemoUser> findByIdForUpdate(@Param("id") UUID id);
}
