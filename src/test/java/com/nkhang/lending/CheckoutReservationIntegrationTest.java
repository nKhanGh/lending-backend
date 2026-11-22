package com.nkhang.lending;

import com.nkhang.lending.checkout.CheckoutService;
import com.nkhang.lending.checkout.OrderRepository;
import com.nkhang.lending.checkout.dto.CheckoutRequest;
import com.nkhang.lending.checkout.dto.CheckoutResponse;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CheckoutReservationIntegrationTest extends PostgresIntegrationTest {

    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HEADPHONES = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID SMART_WATCH = UUID.fromString("00000000-0000-0000-0000-000000000203");

    @Autowired
    CheckoutService checkoutService;

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
    void reservesCreditAndCreatesOneCheckout() {
        CheckoutResponse response = checkoutService.createCheckout(
                new CheckoutRequest(HEADPHONES, 1), "checkout-reserve-001");

        assertThat(response.status()).isEqualTo("RESERVED");
        assertThat(response.amount()).isEqualByComparingTo("120.00");
        assertThat(response.availableCredit()).isEqualByComparingTo("380.00");
        assertThat(orderRepository.count()).isOne();

        CreditAccount account = account();
        assertThat(account.getReservedAmount()).isEqualByComparingTo("120.00");
        assertThat(account.getOutstandingAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void retryWithSameKeyAndPayloadReturnsSameCheckout() {
        CheckoutRequest request = new CheckoutRequest(HEADPHONES, 1);

        CheckoutResponse first = checkoutService.createCheckout(request, "checkout-retry-001");
        CheckoutResponse retry = checkoutService.createCheckout(request, "checkout-retry-001");

        assertThat(retry.checkoutId()).isEqualTo(first.checkoutId());
        assertThat(orderRepository.count()).isOne();
        assertThat(account().getReservedAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void reuseKeyWithDifferentPayloadIsRejected() {
        checkoutService.createCheckout(new CheckoutRequest(HEADPHONES, 1), "checkout-reused-001");

        assertThatThrownBy(() -> checkoutService.createCheckout(
                new CheckoutRequest(HEADPHONES, 2), "checkout-reused-001"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED));

        assertThat(orderRepository.count()).isOne();
        assertThat(account().getReservedAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void insufficientCreditRollsBackEntireCheckout() {
        assertThatThrownBy(() -> checkoutService.createCheckout(
                new CheckoutRequest(HEADPHONES, 5), "checkout-too-large-001"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INSUFFICIENT_CREDIT));

        assertThat(orderRepository.count()).isZero();
        assertThat(account().getReservedAmount()).isEqualByComparingTo("0.00");
        Integer idempotencyRows = jdbcTemplate.queryForObject(
                "select count(*) from idempotency_records", Integer.class);
        assertThat(idempotencyRows).isZero();
    }

    @Test
    void concurrentCheckoutsCannotOverspendCreditLimit() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Callable<Object>> tasks = List.of(
                    reserveAfterSignal(start, "concurrent-checkout-a"),
                    reserveAfterSignal(start, "concurrent-checkout-b")
            );
            List<Future<Object>> futures = tasks.stream().map(executor::submit).toList();
            start.countDown();

            int successCount = 0;
            int insufficientCount = 0;
            for (Future<Object> future : futures) {
                Object result = future.get();
                if (result instanceof CheckoutResponse) {
                    successCount++;
                } else if (result instanceof AppException exception
                        && exception.getErrorCode() == ErrorCode.INSUFFICIENT_CREDIT) {
                    insufficientCount++;
                }
            }

            assertThat(successCount).isOne();
            assertThat(insufficientCount).isOne();
        }

        assertThat(orderRepository.count()).isOne();
        assertThat(account().getReservedAmount()).isEqualByComparingTo("360.00");
        assertThat(account().getAvailableCredit()).isEqualByComparingTo("140.00");
    }

    private Callable<Object> reserveAfterSignal(CountDownLatch start, String key) {
        return () -> {
            start.await();
            try {
                return checkoutService.createCheckout(new CheckoutRequest(SMART_WATCH, 2), key);
            } catch (AppException exception) {
                return exception;
            }
        };
    }

    private CreditAccount account() {
        return creditAccountRepository.findByUserId(DEMO_USER_ID).orElseThrow();
    }
}
