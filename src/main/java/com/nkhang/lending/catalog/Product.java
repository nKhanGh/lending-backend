package com.nkhang.lending.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    UUID id;

    @Column(nullable = false, length = 120)
    String name;

    @Column(length = 500)
    String description;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal price;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(nullable = false)
    boolean active;
}

