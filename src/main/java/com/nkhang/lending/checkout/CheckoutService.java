package com.nkhang.lending.checkout;

import com.nkhang.lending.catalog.Product;
import com.nkhang.lending.catalog.ProductRepository;
import com.nkhang.lending.checkout.dto.CheckoutRequest;
import com.nkhang.lending.checkout.dto.CheckoutResponse;
import com.nkhang.lending.checkout.dto.OrderDetailResponse;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.common.idempotency.IdempotencyOperation;
import com.nkhang.lending.common.idempotency.IdempotencyRecord;
import com.nkhang.lending.common.idempotency.IdempotencyService;
import com.nkhang.lending.common.idempotency.IdempotencyStatus;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
import com.nkhang.lending.instalment.Instalment;
import com.nkhang.lending.instalment.InstalmentRepository;
import com.nkhang.lending.instalment.InstalmentScheduleFactory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutService {

    final ProductRepository productRepository;
    final CreditAccountRepository creditAccountRepository;
    final OrderRepository orderRepository;
    final CreditReservationRepository reservationRepository;
    final IdempotencyService idempotencyService;
    final InstalmentRepository instalmentRepository;
    final InstalmentScheduleFactory scheduleFactory;
    final OrderService orderService;
    final Clock clock;

    @Value("${lending.demo-user-id}")
    UUID demoUserId;

    @Value("${lending.reservation.expiry}")
    Duration reservationExpiry;

    @Transactional
    public CheckoutResponse createCheckout(CheckoutRequest request, String idempotencyKey) {
        String fingerprint = request.productId() + ":" + request.quantity();
        IdempotencyRecord record = idempotencyService.begin(
                IdempotencyOperation.CREATE_CHECKOUT, idempotencyKey, fingerprint);

        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return replayCheckout(record);
        }

        Product product = productRepository.findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

        CreditAccount account = creditAccountRepository.findByUserIdForUpdate(demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
        account.reserve(total);

        Instant now = clock.instant();
        Order order = orderRepository.save(Order.reserved(demoUserId, product, request.quantity(), total, now));
        CreditReservation reservation = reservationRepository.save(
                CreditReservation.active(order, total, now, now.plus(reservationExpiry)));

        idempotencyService.complete(record, order.getId(), 201);
        return CheckoutResponse.from(order, reservation, account);
    }

    @Transactional
    public OrderDetailResponse confirmCheckout(UUID orderId, String idempotencyKey) {
        IdempotencyRecord record = idempotencyService.begin(
                IdempotencyOperation.CONFIRM_CHECKOUT, idempotencyKey, orderId.toString());
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return orderService.getOrder(record.getResourceId());
        }

        Order order = findOrderForUpdate(orderId);
        CreditReservation reservation = findReservationForUpdate(orderId);
        Instant now = clock.instant();
        if (reservation.isExpiredAt(now)) {
            throw new AppException(ErrorCode.RESERVATION_EXPIRED);
        }
        if (order.getStatus() != OrderStatus.RESERVED || reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_CHECKOUT_STATE);
        }

        CreditAccount account = findAccountForUpdate();
        account.capture(reservation.getAmount());
        order.confirm(now);
        reservation.capture(now);

        List<Instalment> schedule = scheduleFactory.create(
                order, now.atZone(ZoneOffset.UTC).toLocalDate());
        instalmentRepository.saveAll(schedule);
        idempotencyService.complete(record, order.getId(), 200);
        return OrderDetailResponse.from(order, reservation, schedule);
    }

    @Transactional
    public OrderDetailResponse cancelCheckout(UUID orderId) {
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return orderService.buildDetail(order);
        }
        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new AppException(ErrorCode.INVALID_CHECKOUT_STATE);
        }

        CreditReservation reservation = findReservationForUpdate(orderId);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_CHECKOUT_STATE);
        }

        Instant now = clock.instant();
        CreditAccount account = findAccountForUpdate();
        account.release(reservation.getAmount());
        order.cancel(now);
        reservation.release(now);
        return OrderDetailResponse.from(order, reservation, List.of());
    }

    private CheckoutResponse replayCheckout(IdempotencyRecord record) {
        Order order = orderRepository.findById(record.getResourceId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Checkout not found"));
        CreditReservation reservation = reservationRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Reservation not found"));
        CreditAccount account = creditAccountRepository.findByUserId(demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
        return CheckoutResponse.from(order, reservation, account);
    }

    private Order findOrderForUpdate(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Checkout not found"));
        if (!order.getUserId().equals(demoUserId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Checkout not found");
        }
        return order;
    }

    private CreditReservation findReservationForUpdate(UUID orderId) {
        return reservationRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Reservation not found"));
    }

    private CreditAccount findAccountForUpdate() {
        return creditAccountRepository.findByUserIdForUpdate(demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
    }
}

