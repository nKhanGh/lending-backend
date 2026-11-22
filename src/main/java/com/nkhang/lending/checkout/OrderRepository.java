package com.nkhang.lending.checkout;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select order from Order order where order.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = "product")
    List<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = "product")
    @Query("select order from Order order where order.id = :id and order.userId = :userId")
    Optional<Order> findDetailByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}

