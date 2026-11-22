package com.nkhang.lending.checkout.dto;

import com.nkhang.lending.checkout.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String currency,
        String status,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant expiredAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getProduct().getId(),
                order.getProduct().getName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getConfirmedAt(),
                order.getCancelledAt(),
                order.getExpiredAt()
        );
    }
}

