package com.nkhang.lending;

import com.nkhang.lending.checkout.CheckoutService;
import com.nkhang.lending.checkout.dto.CheckoutRequest;
import com.nkhang.lending.checkout.dto.CheckoutResponse;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
import com.nkhang.lending.instalment.Instalment;
import com.nkhang.lending.instalment.InstalmentRepository;
import com.nkhang.lending.repayment.RepaymentRepository;
import com.nkhang.lending.repayment.RepaymentService;
import com.nkhang.lending.repayment.dto.RepaymentHistoryResponse;
import com.nkhang.lending.repayment.dto.RepaymentResponse;
import com.nkhang.lending.repayment.dto.SettlementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "lending.reservation.cleanup-delay=1h")
class RepaymentIntegrationTest extends PostgresIntegrationTest {

    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HEADPHONES = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Autowired
    CheckoutService checkoutService;

    @Autowired
    RepaymentService repaymentService;

    @Autowired
    RepaymentRepository repaymentRepository;

    @Autowired
    InstalmentRepository instalmentRepository;

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
    void payingFirstInstalmentRestoresAvailableCredit() {
        List<Instalment> schedule = confirmedSchedule("pay-first");

        RepaymentResponse response = repaymentService.pay(
                schedule.getFirst().getId(), "repay-first-001");

        assertThat(response.amount()).isEqualByComparingTo("30.00");
        assertThat(response.status()).isEqualTo("PAID");
        assertThat(response.outstandingAmount()).isEqualByComparingTo("90.00");
        assertThat(response.availableCredit()).isEqualByComparingTo("410.00");
        assertThat(repaymentRepository.count()).isOne();
    }

    @Test
    void repaymentRetryDoesNotApplyFinancialEffectTwice() {
        UUID instalmentId = confirmedSchedule("pay-retry").getFirst().getId();

        RepaymentResponse first = repaymentService.pay(instalmentId, "repay-retry-001");
        RepaymentResponse retry = repaymentService.pay(instalmentId, "repay-retry-001");

        assertThat(retry.repaymentId()).isEqualTo(first.repaymentId());
        assertThat(repaymentRepository.count()).isOne();
        assertThat(account().getOutstandingAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    void payingPaidInstalmentWithNewKeyIsRejected() {
        UUID instalmentId = confirmedSchedule("pay-duplicate").getFirst().getId();
        repaymentService.pay(instalmentId, "repay-duplicate-a");

        assertThatThrownBy(() -> repaymentService.pay(instalmentId, "repay-duplicate-b"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INSTALMENT_ALREADY_PAID));

        assertThat(repaymentRepository.count()).isOne();
        assertThat(account().getOutstandingAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    void concurrentPaymentRequestsSubtractOutstandingOnlyOnce() throws Exception {
        UUID instalmentId = confirmedSchedule("pay-concurrent").getFirst().getId();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Callable<Object>> tasks = List.of(
                    payAfterSignal(start, instalmentId, "pay-concurrent-a"),
                    payAfterSignal(start, instalmentId, "pay-concurrent-b")
            );
            List<Future<Object>> futures = tasks.stream().map(executor::submit).toList();
            start.countDown();

            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            assertThat(results.stream().filter(RepaymentResponse.class::isInstance)).hasSize(1);
            assertThat(results.stream()
                    .filter(AppException.class::isInstance)
                    .map(AppException.class::cast)
                    .map(AppException::getErrorCode))
                    .containsExactly(ErrorCode.INSTALMENT_ALREADY_PAID);
        }

        assertThat(repaymentRepository.count()).isOne();
        assertThat(account().getOutstandingAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    void payingAllInstalmentsClearsDebtAndHistoryListsPayments() {
        List<Instalment> schedule = confirmedSchedule("pay-all");
        for (int index = 0; index < schedule.size(); index++) {
            repaymentService.pay(schedule.get(index).getId(), "repay-all-" + index);
        }

        CreditAccount account = account();
        assertThat(account.getOutstandingAmount()).isEqualByComparingTo("0.00");
        assertThat(account.getAvailableCredit()).isEqualByComparingTo("500.00");
        List<RepaymentHistoryResponse> history = repaymentService.getHistory();
        assertThat(history).hasSize(4);
        assertThat(history).extracting(RepaymentHistoryResponse::amount)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("30.00"));
    }

    @Test
    void earlySettlementPaysOnlyRemainingInstalmentsAndRetryIsStable() {
        List<Instalment> schedule = confirmedSchedule("settle-partial");
        UUID orderId = schedule.getFirst().getOrder().getId();
        repaymentService.pay(schedule.getFirst().getId(), "settle-first-payment");

        SettlementResponse first = repaymentService.settleOrder(orderId, "settle-order-001");
        SettlementResponse retry = repaymentService.settleOrder(orderId, "settle-order-001");

        assertThat(first.instalmentsPaid()).isEqualTo(3);
        assertThat(first.amount()).isEqualByComparingTo("90.00");
        assertThat(first.repaymentIds()).hasSize(3);
        assertThat(retry).isEqualTo(first);
        assertThat(repaymentRepository.count()).isEqualTo(4);
        assertThat(account().getOutstandingAmount()).isEqualByComparingTo("0.00");
        assertThat(account().getAvailableCredit()).isEqualByComparingTo("500.00");

        assertThatThrownBy(() -> repaymentService.settleOrder(orderId, "settle-order-new-key"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.ORDER_ALREADY_SETTLED));
    }

    @Test
    void settlementRacingWithSinglePaymentNeverDoublePays() throws Exception {
        List<Instalment> schedule = confirmedSchedule("settle-race");
        UUID orderId = schedule.getFirst().getOrder().getId();
        UUID instalmentId = schedule.getFirst().getId();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Object> settlement = executor.submit(() -> {
                start.await();
                return repaymentService.settleOrder(orderId, "settle-race-key");
            });
            Future<Object> single = executor.submit(payAfterSignal(start, instalmentId, "settle-race-single"));
            start.countDown();

            assertThat(settlement.get()).isInstanceOf(SettlementResponse.class);
            Object singleResult = single.get();
            assertThat(singleResult instanceof RepaymentResponse
                    || singleResult instanceof AppException exception
                    && exception.getErrorCode() == ErrorCode.INSTALMENT_ALREADY_PAID).isTrue();
        }

        assertThat(repaymentRepository.count()).isEqualTo(4);
        assertThat(account().getOutstandingAmount()).isEqualByComparingTo("0.00");
        assertThat(account().getAvailableCredit()).isEqualByComparingTo("500.00");
    }

    private List<Instalment> confirmedSchedule(String keyPrefix) {
        CheckoutResponse checkout = checkoutService.createCheckout(
                new CheckoutRequest(HEADPHONES, 1), keyPrefix + "-reserve");
        checkoutService.confirmCheckout(checkout.checkoutId(), keyPrefix + "-confirm");
        return instalmentRepository.findAllByOrderIdOrderBySequenceNoAsc(checkout.checkoutId());
    }

    private Callable<Object> payAfterSignal(CountDownLatch start, UUID instalmentId, String key) {
        return () -> {
            start.await();
            try {
                return repaymentService.pay(instalmentId, key);
            } catch (AppException exception) {
                return exception;
            }
        };
    }

    private CreditAccount account() {
        return creditAccountRepository.findByUserId(DEMO_USER_ID).orElseThrow();
    }
}
