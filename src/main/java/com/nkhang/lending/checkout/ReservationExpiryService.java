package com.nkhang.lending.checkout;

import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReservationExpiryService {

    OrderRepository orderRepository;
    CreditReservationRepository reservationRepository;
    CreditAccountRepository creditAccountRepository;
    Clock clock;

    @Transactional(readOnly = true)
    public List<UUID> findDueOrderIds() {
        return reservationRepository
                .findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                        ReservationStatus.ACTIVE, clock.instant())
                .stream()
                .map(reservation -> reservation.getOrder().getId())
                .toList();
    }

    @Transactional
    public void expire(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.RESERVED) {
            return;
        }

        CreditReservation reservation = reservationRepository.findByOrderIdForUpdate(orderId).orElse(null);
        Instant now = clock.instant();
        if (reservation == null
                || reservation.getStatus() != ReservationStatus.ACTIVE
                || !reservation.isExpiredAt(now)) {
            return;
        }

        CreditAccount account = creditAccountRepository.findByUserIdForUpdate(order.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
        account.release(reservation.getAmount());
        order.expire(now);
        reservation.expire(now);
    }
}

