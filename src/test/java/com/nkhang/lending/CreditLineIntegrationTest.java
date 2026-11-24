package com.nkhang.lending;

import com.nkhang.lending.application.dto.CreditApplicationRequest;
import com.nkhang.lending.application.dto.CreditApplicationResponse;
import com.nkhang.lending.application.CreditApplicationService;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.credit.CreditService;
import com.nkhang.lending.drawdown.CreditDrawdownService;
import com.nkhang.lending.drawdown.dto.DrawdownDetailResponse;
import com.nkhang.lending.drawdown.dto.DrawdownRequest;
import com.nkhang.lending.instalment.OverdueService;
import com.nkhang.lending.ledger.ReconciliationService;
import com.nkhang.lending.repayment.PaymentAttemptStatus;
import com.nkhang.lending.repayment.dto.PaymentAttemptResponse;
import com.nkhang.lending.repayment.RepaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CUSTOMER")
class CreditLineIntegrationTest extends PostgresIntegrationTest {
    @Autowired CreditApplicationService applicationService;
    @Autowired CreditService creditService;
    @Autowired CreditDrawdownService drawdownService;
    @Autowired RepaymentService repaymentService;
    @Autowired OverdueService overdueService;
    @Autowired ReconciliationService reconciliationService;

    @Test
    void rejectedApplicationCanBeReassessedButApprovedAccountCannotBeDuplicated() {
        CreditApplicationResponse rejected = applicationService.submit(
                new CreditApplicationRequest(money("1000"), money("400"), money("300.01"), null));
        assertThat(rejected.decision()).isEqualTo("REJECTED");
        assertThat(rejected.monthlySurplus()).isEqualByComparingTo("299.99");
        assertThat(applicationService.latest().applicationId()).isEqualTo(rejected.applicationId());

        CreditApplicationResponse approved = approve();
        assertThat(approved.decision()).isEqualTo("APPROVED");
        assertThat(approved.reasonCode()).isEqualTo("APPROVED_TIER_1000");
        assertThat(approved.policyVersion()).isEqualTo("DEMO_V2");
        assertThat(creditService.getAccount().availableLimit()).isEqualByComparingTo("1000.00");
        assertThatThrownBy(this::approve).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.APPLICATION_ALREADY_APPROVED));
    }

    @Test
    void drawdownAndRepaymentsRestoreLimitAndCloseTheLoan() {
        approve();
        DrawdownDetailResponse drawdown = drawdownService.create(new DrawdownRequest(money("800.00")), "draw-800");
        assertThat(drawdown.instalments()).hasSize(4);
        assertThat(drawdown.instalments()).extracting(item -> item.amount())
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("200.00"));
        assertThat(creditService.getAccount().usedLimit()).isEqualByComparingTo("800.00");
        assertThat(creditService.getAccount().availableLimit()).isEqualByComparingTo("200.00");

        DrawdownDetailResponse retry = drawdownService.create(new DrawdownRequest(money("800.0")), "draw-800");
        assertThat(retry.drawdownId()).isEqualTo(drawdown.drawdownId());
        assertThat(drawdownService.list()).hasSize(1);
        assertThatThrownBy(() -> drawdownService.create(new DrawdownRequest(money("700.00")), "draw-800"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED));

        UUID firstId = drawdown.instalments().getFirst().id();
        PaymentAttemptResponse first = repaymentService.request(firstId, "repay-first");
        assertThat(first.status()).isEqualTo("PENDING");
        assertThat(creditService.getAccount().usedLimit()).isEqualByComparingTo("800.00");
        assertThat(repaymentService.request(firstId, "repay-first")).isEqualTo(first);
        assertThatThrownBy(() -> repaymentService.request(firstId, "repay-first-new-key"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_PENDING));
        repaymentService.settle(first.paymentAttemptId(), PaymentAttemptStatus.SUCCEEDED, "evt-first");
        assertThat(repaymentService.request(firstId, "repay-first").status()).isEqualTo("SUCCEEDED");
        assertThat(repaymentService.settle(first.paymentAttemptId(), PaymentAttemptStatus.SUCCEEDED, "evt-first").status())
                .isEqualTo("SUCCEEDED");
        assertThatThrownBy(() -> repaymentService.settle(first.paymentAttemptId(), PaymentAttemptStatus.FAILED, "evt-other"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_RESOLVED));
        assertThatThrownBy(() -> repaymentService.request(firstId, "repay-first-new-key"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSTALMENT_ALREADY_PAID));
        assertThat(drawdownService.detail(drawdown.drawdownId()).outstandingPrincipal()).isEqualByComparingTo("600.00");
        assertThat(creditService.getAccount().availableLimit()).isEqualByComparingTo("400.00");

        for (int index = 1; index < 4; index++) {
            PaymentAttemptResponse attempt = repaymentService.request(drawdown.instalments().get(index).id(), "repay-" + index);
            repaymentService.settle(attempt.paymentAttemptId(), PaymentAttemptStatus.SUCCEEDED, "evt-" + index);
        }
        assertThat(drawdownService.detail(drawdown.drawdownId()).status()).isEqualTo("PAID_OFF");
        assertThat(drawdownService.detail(drawdown.drawdownId()).outstandingPrincipal()).isEqualByComparingTo("0.00");
        assertThat(creditService.getAccount().availableLimit()).isEqualByComparingTo("1000.00");
        assertThat(repaymentService.history(drawdown.drawdownId())).hasSize(4);
        assertThat(jdbcTemplate.queryForObject("select sum(amount) from repayment_transactions", BigDecimal.class))
                .isEqualByComparingTo("800.00");
    }

    @Test
    void invalidDrawdownAndOutOfOrderRepaymentDoNotChangeBalances() {
        approve();
        assertThatThrownBy(() -> drawdownService.create(new DrawdownRequest(money("1200.00")), "too-much"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_LIMIT));
        assertThat(drawdownService.list()).isEmpty();
        assertThat(creditService.getAccount().usedLimit()).isEqualByComparingTo("0.00");

        DrawdownDetailResponse drawdown = drawdownService.create(new DrawdownRequest(money("100.01")), "rounding");
        assertThat(drawdown.instalments()).extracting(item -> item.amount())
                .containsExactly(money("25.00"), money("25.00"), money("25.00"), money("25.01"));
        assertThatThrownBy(() -> repaymentService.request(drawdown.instalments().get(1).id(), "wrong-order"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSTALMENT_OUT_OF_ORDER));
        assertThat(creditService.getAccount().usedLimit()).isEqualByComparingTo("100.01");
        assertThat(repaymentService.history(drawdown.drawdownId())).isEmpty();
    }

    @Test
    void failedPaymentDoesNotRestoreLimitAndCanBeRetried() {
        approve();
        DrawdownDetailResponse drawdown = drawdownService.create(new DrawdownRequest(money("400.00")), "draw-failed");
        UUID instalmentId = drawdown.instalments().getFirst().id();
        PaymentAttemptResponse first = repaymentService.request(instalmentId, "attempt-one");
        repaymentService.settle(first.paymentAttemptId(), PaymentAttemptStatus.FAILED, "evt-failed");
        assertThat(creditService.getAccount().usedLimit()).isEqualByComparingTo("400.00");
        assertThat(repaymentService.history(drawdown.drawdownId())).isEmpty();
        PaymentAttemptResponse retry = repaymentService.request(instalmentId, "attempt-two");
        assertThat(retry.paymentAttemptId()).isNotEqualTo(first.paymentAttemptId());
        repaymentService.settle(retry.paymentAttemptId(), PaymentAttemptStatus.SUCCEEDED, "evt-succeeded");
        assertThat(creditService.getAccount().usedLimit()).isEqualByComparingTo("300.00");
        assertThat(repaymentService.history(drawdown.drawdownId())).hasSize(1);
    }

    @Test
    void partialPaymentsReducePrincipalAndKeepInstalmentOpenUntilFullyPaid() {
        approve();
        DrawdownDetailResponse drawdown = drawdownService.create(new DrawdownRequest(money("400.00")), "draw-partial");
        UUID firstId = drawdown.instalments().getFirst().id();
        UUID secondId = drawdown.instalments().get(1).id();
        PaymentAttemptResponse partial = repaymentService.request(firstId, "partial-40", money("40.00"));
        assertThat(partial.amount()).isEqualByComparingTo("40.00");
        repaymentService.settle(partial.paymentAttemptId(), PaymentAttemptStatus.SUCCEEDED, "evt-partial-40");
        assertThat(creditService.getAccount().usedLimit()).isEqualByComparingTo("360.00");
        assertThat(drawdownService.detail(drawdown.drawdownId()).instalments().getFirst().remainingAmount())
                .isEqualByComparingTo("60.00");
        assertThat(drawdownService.detail(drawdown.drawdownId()).instalments().getFirst().status()).isEqualTo("UNPAID");
        assertThatThrownBy(() -> repaymentService.request(secondId, "second-too-early"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSTALMENT_OUT_OF_ORDER));
        assertThatThrownBy(() -> repaymentService.request(firstId, "too-much", money("60.01")))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        PaymentAttemptResponse remainder = repaymentService.request(firstId, "remainder");
        assertThat(remainder.amount()).isEqualByComparingTo("60.00");
        repaymentService.settle(remainder.paymentAttemptId(), PaymentAttemptStatus.SUCCEEDED, "evt-remainder");
        assertThat(creditService.getAccount().usedLimit()).isEqualByComparingTo("300.00");
        assertThat(drawdownService.detail(drawdown.drawdownId()).instalments().getFirst().status()).isEqualTo("PAID");
        assertThat(repaymentService.history(drawdown.drawdownId())).hasSize(2);
    }

    @Test
    void overdueJobAndPartialPaymentPreserveOutstandingDaysUntilFullyPaid() {
        approve();
        DrawdownDetailResponse drawdown = drawdownService.create(new DrawdownRequest(money("400.00")), "draw-overdue");
        UUID firstId = drawdown.instalments().getFirst().id();
        jdbcTemplate.update("update instalments set due_date = ? where id = ?",
                Date.valueOf(LocalDate.now(ZoneOffset.UTC).minusDays(3)), firstId);
        assertThat(overdueService.markOverdue()).isEqualTo(1);
        assertThat(overdueService.markOverdue()).isZero();
        assertThat(drawdownService.detail(drawdown.drawdownId()).instalments().getFirst().status()).isEqualTo("OVERDUE");
        assertThat(drawdownService.detail(drawdown.drawdownId()).instalments().getFirst().daysOverdue()).isGreaterThanOrEqualTo(3);
        PaymentAttemptResponse partial = repaymentService.request(firstId, "late-partial", money("40.00"));
        repaymentService.settle(partial.paymentAttemptId(), PaymentAttemptStatus.SUCCEEDED, "evt-late-partial");
        assertThat(drawdownService.detail(drawdown.drawdownId()).instalments().getFirst().status()).isEqualTo("OVERDUE");
        assertThat(drawdownService.detail(drawdown.drawdownId()).instalments().getFirst().remainingAmount())
                .isEqualByComparingTo("60.00");
        PaymentAttemptResponse remainder = repaymentService.request(firstId, "late-remainder");
        repaymentService.settle(remainder.paymentAttemptId(), PaymentAttemptStatus.SUCCEEDED, "evt-late-remainder");
        assertThat(drawdownService.detail(drawdown.drawdownId()).instalments().getFirst().status()).isEqualTo("PAID");
        assertThat(drawdownService.detail(drawdown.drawdownId()).instalments().getFirst().daysOverdue()).isZero();
    }

    @Test
    void ledgerReconcilesWithAccountAndDrawdownAfterEachSettlement() {
        approve();
        DrawdownDetailResponse drawdown = drawdownService.create(new DrawdownRequest(money("400.00")), "ledger-draw");
        assertThat(reconciliationService.accounts()).singleElement().satisfies(row -> {
            assertThat(row.balanced()).isTrue();
            assertThat(row.ledgerOutstanding()).isEqualByComparingTo("400.00");
        });
        PaymentAttemptResponse attempt = repaymentService.request(drawdown.instalments().getFirst().id(),
                "ledger-pay", money("30.00"));
        repaymentService.settle(attempt.paymentAttemptId(), PaymentAttemptStatus.SUCCEEDED, "evt-ledger");
        assertThat(reconciliationService.accounts()).singleElement().satisfies(row -> {
            assertThat(row.balanced()).isTrue();
            assertThat(row.usedLimit()).isEqualByComparingTo("370.00");
            assertThat(row.ledgerOutstanding()).isEqualByComparingTo("370.00");
        });
        assertThat(jdbcTemplate.queryForObject("select count(*) from ledger_journals", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from ledger_entries", Integer.class)).isEqualTo(4);
        assertThat(reconciliationService.accounts()).singleElement()
                .satisfies(row -> assertThat(row.unbalancedJournals()).isZero());
        jdbcTemplate.update("update credit_accounts set used_limit = 0");
        assertThat(reconciliationService.accounts()).singleElement()
                .satisfies(row -> assertThat(row.balanced()).isFalse());
    }

    @Test
    void concurrentDrawdownsCannotExceedLimit() throws Exception {
        approve();
        CountDownLatch start = new CountDownLatch(1);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Object> first = executor.submit(() -> tryDrawAsUser(start, "race-one", securityContext));
            Future<Object> second = executor.submit(() -> tryDrawAsUser(start, "race-two", securityContext));
            start.countDown();
            Object firstResult = first.get();
            Object secondResult = second.get();
            assertThat((firstResult instanceof DrawdownDetailResponse ? 1 : 0)
                    + (secondResult instanceof DrawdownDetailResponse ? 1 : 0)).isEqualTo(1);
            assertThat(firstResult instanceof AppException || secondResult instanceof AppException).isTrue();
        }
        assertThat(drawdownService.list()).hasSize(1);
        assertThat(creditService.getAccount().usedLimit()).isEqualByComparingTo("700.00");
        assertThat(creditService.getAccount().availableLimit()).isEqualByComparingTo("300.00");
    }

    private Object tryDrawAsUser(CountDownLatch start, String key, SecurityContext context)
            throws InterruptedException {
        SecurityContextHolder.setContext(context);
        try {
            return tryDraw(start, key);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Object tryDraw(CountDownLatch start, String key) throws InterruptedException {
        start.await();
        try {
            return drawdownService.create(new DrawdownRequest(money("700.00")), key);
        } catch (AppException exception) {
            return exception;
        }
    }

    private CreditApplicationResponse approve() {
        return applicationService.submit(new CreditApplicationRequest(
                money("3000.00"), money("1400.00"), money("500.00"), "SGD"));
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
