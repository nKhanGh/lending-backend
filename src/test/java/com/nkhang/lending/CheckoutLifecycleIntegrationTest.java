package com.nkhang.lending;

import com.nkhang.lending.checkout.CheckoutService;
import com.nkhang.lending.checkout.Order;
import com.nkhang.lending.checkout.OrderRepository;
import com.nkhang.lending.checkout.OrderStatus;
import com.nkhang.lending.checkout.ReservationExpiryService;
import com.nkhang.lending.checkout.dto.CheckoutRequest;
import com.nkhang.lending.checkout.dto.CheckoutResponse;
import com.nkhang.lending.checkout.dto.OrderDetailResponse;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
import com.nkhang.lending.instalment.Instalment;
import com.nkhang.lending.instalment.InstalmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "lending.reservation.cleanup-delay=1h")
class CheckoutLifecycleIntegrationTest extends PostgresIntegrationTest {

    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HEADPHONES = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID KEYBOARD = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Autowired
    CheckoutService checkoutService;

    @Autowired
    ReservationExpiryService expiryService;

    @Autowired
    InstalmentRepository instalmentRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    CreditAccountRepository creditAccountRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetFinancialState() {
        jdbcTemplate.update("delete from repayments");
        jdbcTemplate.update("delete from instalments");
        jdbcTemplate.update("delete from credit_reservations");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from idempotency_records");
        jdbcTemplate.update("""
                update credit_accounts
                set outstanding_amount = 0.00, reserved_amount = 0.00, version = 0
                where user_id = ?
                """, DEMO_USER_ID);
    }

    @Test
    void confirmCapturesReservationAndCreatesFourInstalments() {
        CheckoutResponse checkout = reserve(HEADPHONES, "lifecycle-reserve-001");

        OrderDetailResponse confirmed = checkoutService.confirmCheckout(
                checkout.checkoutId(), "lifecycle-confirm-001");

        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(confirmed.reservationStatus()).isEqualTo("CAPTURED");
        assertThat(confirmed.instalments()).hasSize(4);
        assertThat(confirmed.instalments())
                .extracting(item -> item.amount())
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("30.00"));

        CreditAccount account = account();
        assertThat(account.getReservedAmount()).isEqualByComparingTo("0.00");
        assertThat(account.getOutstandingAmount()).isEqualByComparingTo("120.00");
        assertThat(account.getAvailableCredit()).isEqualByComparingTo("380.00");
    }

    @Test
    void confirmRetryDoesNotCreateDuplicateSchedule() {
        CheckoutResponse checkout = reserve(HEADPHONES, "confirm-retry-reserve-001");

        OrderDetailResponse first = checkoutService.confirmCheckout(
                checkout.checkoutId(), "confirm-retry-001");
        OrderDetailResponse retry = checkoutService.confirmCheckout(
                checkout.checkoutId(), "confirm-retry-001");

        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(instalmentRepository.countByOrderId(checkout.checkoutId())).isEqualTo(4);
        assertThat(account().getOutstandingAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void cancelReleasesCreditExactlyOnceAndCannotBeConfirmed() {
        CheckoutResponse checkout = reserve(HEADPHONES, "cancel-reserve-001");

        OrderDetailResponse cancelled = checkoutService.cancelCheckout(checkout.checkoutId());
        OrderDetailResponse retry = checkoutService.cancelCheckout(checkout.checkoutId());

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(retry.status()).isEqualTo("CANCELLED");
        assertThat(account().getAvailableCredit()).isEqualByComparingTo("500.00");

        assertThatThrownBy(() -> checkoutService.confirmCheckout(
                checkout.checkoutId(), "confirm-cancelled-001"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_CHECKOUT_STATE));
        assertThat(instalmentRepository.countByOrderId(checkout.checkoutId())).isZero();
    }

    @Test
    void expiredReservationCannotBeConfirmedAndIsReleased() {
        CheckoutResponse checkout = reserve(HEADPHONES, "expiry-reserve-001");
        jdbcTemplate.update(
                "update credit_reservations set expires_at = ? where order_id = ?",
                Timestamp.from(Instant.now().minusSeconds(5)), checkout.checkoutId());

        assertThatThrownBy(() -> checkoutService.confirmCheckout(
                checkout.checkoutId(), "confirm-expired-001"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESERVATION_EXPIRED));

        expiryService.expire(checkout.checkoutId());

        Order order = orderRepository.findById(checkout.checkoutId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(account().getAvailableCredit()).isEqualByComparingTo("500.00");
    }

    @Test
    void roundingRemainderIsAssignedToFinalInstalment() {
        CheckoutResponse checkout = reserve(KEYBOARD, "rounding-reserve-001");
        checkoutService.confirmCheckout(checkout.checkoutId(), "rounding-confirm-001");

        List<Instalment> instalments = instalmentRepository
                .findAllByOrderIdOrderBySequenceNoAsc(checkout.checkoutId());
        assertThat(instalments).extracting(Instalment::getAmount)
                .containsExactly(
                        new BigDecimal("25.00"),
                        new BigDecimal("25.00"),
                        new BigDecimal("25.00"),
                        new BigDecimal("25.01"));
        assertThat(instalments.stream().map(Instalment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.01");
    }

    private CheckoutResponse reserve(UUID productId, String key) {
        return checkoutService.createCheckout(new CheckoutRequest(productId, 1), key);
    }

    private CreditAccount account() {
        return creditAccountRepository.findByUserId(DEMO_USER_ID).orElseThrow();
    }
}
