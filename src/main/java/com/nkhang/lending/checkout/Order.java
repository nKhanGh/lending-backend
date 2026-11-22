package com.nkhang.lending.checkout;

import com.nkhang.lending.catalog.Product;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(nullable = false)
    int quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    OrderStatus status;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "confirmed_at")
    Instant confirmedAt;

    @Column(name = "cancelled_at")
    Instant cancelledAt;

    @Column(name = "expired_at")
    Instant expiredAt;

    public static Order reserved(UUID userId, Product product, int quantity, BigDecimal total, Instant now) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.userId = userId;
        order.product = product;
        order.quantity = quantity;
        order.unitPrice = product.getPrice();
        order.totalAmount = total;
        order.currency = product.getCurrency();
        order.status = OrderStatus.RESERVED;
        order.createdAt = now;
        return order;
    }

    public void confirm(Instant now) {
        requireReserved();
        status = OrderStatus.CONFIRMED;
        confirmedAt = now;
    }

    public void cancel(Instant now) {
        requireReserved();
        status = OrderStatus.CANCELLED;
        cancelledAt = now;
    }

    public void expire(Instant now) {
        requireReserved();
        status = OrderStatus.EXPIRED;
        expiredAt = now;
    }

    private void requireReserved() {
        if (status != OrderStatus.RESERVED) {
            throw new AppException(ErrorCode.INVALID_CHECKOUT_STATE);
        }
    }
}

