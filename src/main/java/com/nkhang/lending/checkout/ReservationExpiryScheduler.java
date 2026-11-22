package com.nkhang.lending.checkout;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReservationExpiryScheduler {

    ReservationExpiryService reservationExpiryService;

    @Scheduled(fixedDelayString = "${lending.reservation.cleanup-delay}")
    public void expireReservations() {
        reservationExpiryService.findDueOrderIds().forEach(orderId -> {
            try {
                reservationExpiryService.expire(orderId);
            } catch (RuntimeException exception) {
                log.error("Failed to expire reservation for order {}", orderId, exception);
            }
        });
    }
}

