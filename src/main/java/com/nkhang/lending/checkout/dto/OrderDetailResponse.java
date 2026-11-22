package com.nkhang.lending.checkout.dto;

import com.nkhang.lending.checkout.CreditReservation;
import com.nkhang.lending.checkout.Order;
import com.nkhang.lending.instalment.Instalment;
import com.nkhang.lending.instalment.dto.InstalmentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
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
        Instant expiredAt,
        UUID reservationId,
        String reservationStatus,
        Instant reservationExpiresAt,
        List<InstalmentResponse> instalments
) {
    public static OrderDetailResponse from(
            Order order, CreditReservation reservation, List<Instalment> instalments) {
        return new OrderDetailResponse(
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
                order.getExpiredAt(),
                reservation.getId(),
                reservation.getStatus().name(),
                reservation.getExpiresAt(),
                instalments.stream().map(InstalmentResponse::from).toList()
        );
    }
}

