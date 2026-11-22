package com.nkhang.lending.checkout.dto;

import com.nkhang.lending.checkout.CreditReservation;
import com.nkhang.lending.checkout.Order;
import com.nkhang.lending.credit.CreditAccount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CheckoutResponse(
        UUID checkoutId,
        String status,
        BigDecimal amount,
        String currency,
        Instant expiresAt,
        BigDecimal availableCredit
) {
    public static CheckoutResponse from(
            Order order, CreditReservation reservation, CreditAccount account) {
        return new CheckoutResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCurrency(),
                reservation.getExpiresAt(),
                account.getAvailableCredit()
        );
    }
}

