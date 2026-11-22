package com.nkhang.lending.checkout;

import com.nkhang.lending.catalog.Product;
import com.nkhang.lending.catalog.ProductRepository;
import com.nkhang.lending.checkout.dto.CheckoutRequest;
import com.nkhang.lending.checkout.dto.CheckoutResponse;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.common.idempotency.IdempotencyOperation;
import com.nkhang.lending.common.idempotency.IdempotencyRecord;
import com.nkhang.lending.common.idempotency.IdempotencyService;
import com.nkhang.lending.common.idempotency.IdempotencyStatus;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
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

    private CheckoutResponse replayCheckout(IdempotencyRecord record) {
        Order order = orderRepository.findById(record.getResourceId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Checkout not found"));
        CreditReservation reservation = reservationRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Reservation not found"));
        CreditAccount account = creditAccountRepository.findByUserId(demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
        return CheckoutResponse.from(order, reservation, account);
    }
}

