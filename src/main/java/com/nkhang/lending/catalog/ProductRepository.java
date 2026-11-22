package com.nkhang.lending.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByActiveTrueOrderByNameAsc();

    Optional<Product> findByIdAndActiveTrue(UUID id);
}

