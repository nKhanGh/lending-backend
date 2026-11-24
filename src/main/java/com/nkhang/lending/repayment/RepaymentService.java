package com.nkhang.lending.repayment;

import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.common.idempotency.IdempotencyOperation;
import com.nkhang.lending.common.idempotency.IdempotencyRecord;
import com.nkhang.lending.common.idempotency.IdempotencyService;
import com.nkhang.lending.common.idempotency.IdempotencyStatus;
import com.nkhang.lending.config.CurrentUser;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
import com.nkhang.lending.drawdown.CreditDrawdown;
import com.nkhang.lending.drawdown.CreditDrawdownRepository;
import com.nkhang.lending.drawdown.DrawdownStatus;
import com.nkhang.lending.instalment.Instalment;
import com.nkhang.lending.instalment.InstalmentRepository;
import com.nkhang.lending.instalment.InstalmentStatus;
import com.nkhang.lending.ledger.LedgerService;
import com.nkhang.lending.repayment.dto.PaymentAttemptResponse;
import com.nkhang.lending.repayment.dto.RepaymentHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepaymentService {
    private final CreditAccountRepository accountRepository;
    private final CreditDrawdownRepository drawdownRepository;
    private final InstalmentRepository instalmentRepository;
    private final RepaymentTransactionRepository repaymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Transactional
    public PaymentAttemptResponse request(UUID instalmentId, String key) {
        return request(instalmentId, key, null);
    }

    @Transactional
    public PaymentAttemptResponse request(UUID instalmentId, String key, BigDecimal requestedAmount) {
        if (requestedAmount != null && (requestedAmount.scale() > 2 || requestedAmount.signum() <= 0)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Payment amount must be positive with two decimals");
        }
        UUID userId = currentUser.id();
        IdempotencyRecord record = idempotencyService.begin(
                IdempotencyOperation.REPAY_INSTALMENT, userId, key,
                instalmentId + ":" + (requestedAmount == null ? "REMAINING" : requestedAmount.setScale(2).toPlainString()));
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return ownedAttempt(record.getResourceId(), userId);
        }
        // The account lock serializes requests for the same user's balance.
        CreditAccount account = accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
        Instalment candidate = instalmentRepository.findById(instalmentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Instalment not found"));
        if (!candidate.getDrawdown().getAccount().getId().equals(account.getId())) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Instalment not found");
        }
        CreditDrawdown drawdown = drawdownRepository.findByIdForUpdate(candidate.getDrawdown().getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Drawdown not found"));
        Instalment instalment = instalmentRepository.findByIdForUpdate(instalmentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Instalment not found"));
        if (instalment.getStatus() == InstalmentStatus.PAID) {
            throw new AppException(ErrorCode.INSTALMENT_ALREADY_PAID);
        }
        if (drawdown.getStatus() != DrawdownStatus.ACTIVE) {
            throw new AppException(ErrorCode.DRAWDOWN_NOT_ACTIVE);
        }
        Instalment earliest = instalmentRepository.findFirstByDrawdownIdAndStatusInOrderBySequenceNoAsc(
                drawdown.getId(), List.of(InstalmentStatus.UNPAID, InstalmentStatus.OVERDUE))
                .orElseThrow(() -> new AppException(ErrorCode.DRAWDOWN_NOT_ACTIVE));
        if (!earliest.getId().equals(instalmentId)) {
            throw new AppException(ErrorCode.INSTALMENT_OUT_OF_ORDER);
        }
        if (attemptRepository.existsByInstalmentIdAndStatus(instalmentId, PaymentAttemptStatus.PENDING)) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PENDING);
        }
        BigDecimal amount = requestedAmount == null ? instalment.getRemainingAmount() : requestedAmount.setScale(2);
        if (amount.signum() <= 0 || amount.compareTo(instalment.getRemainingAmount()) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Payment exceeds remaining instalment amount");
        }
        PaymentAttempt attempt = attemptRepository.save(PaymentAttempt.pending(
                userId, instalment, amount, clock.instant().truncatedTo(ChronoUnit.MICROS)));
        idempotencyService.complete(record, attempt.getId(), 202);
        return PaymentAttemptResponse.from(attempt);
    }

    @Transactional
    public PaymentAttemptResponse settle(UUID attemptId, PaymentAttemptStatus outcome, String eventId) {
        if (outcome == PaymentAttemptStatus.PENDING || eventId == null || eventId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        PaymentAttempt candidate = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Payment attempt not found"));
        // Lock order is account -> drawdown -> instalment -> attempt.
        CreditAccount account = accountRepository.findByUserIdForUpdate(candidate.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
        CreditDrawdown drawdown = drawdownRepository.findByIdForUpdate(candidate.getInstalment().getDrawdown().getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Drawdown not found"));
        Instalment instalment = instalmentRepository.findByIdForUpdate(candidate.getInstalment().getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Instalment not found"));
        PaymentAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Payment attempt not found"));
        if (attempt.getStatus() != PaymentAttemptStatus.PENDING) {
            if (attempt.getStatus() == outcome && eventId.equals(attempt.getProviderEventId())) {
                return PaymentAttemptResponse.from(attempt);
            }
            throw new AppException(ErrorCode.PAYMENT_ALREADY_RESOLVED);
        }
        if (attemptRepository.existsByProviderEventId(eventId)) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_RESOLVED);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        if (outcome == PaymentAttemptStatus.SUCCEEDED) {
            if (instalment.getStatus() == InstalmentStatus.PAID || drawdown.getStatus() != DrawdownStatus.ACTIVE) {
                throw new AppException(ErrorCode.PAYMENT_ALREADY_RESOLVED);
            }
            instalment.applyPayment(attempt.getAmount(), now);
            drawdown.repay(attempt.getAmount());
            account.repay(attempt.getAmount());
            RepaymentTransaction repayment = repaymentRepository.save(
                    RepaymentTransaction.success(instalment, attempt.getAmount(), now));
            ledgerService.repayment(attempt.getUserId(), repayment.getId(), attempt.getAmount());
        }
        attempt.resolve(outcome, eventId, now);
        return PaymentAttemptResponse.from(attempt);
    }

    @Transactional(readOnly = true)
    public PaymentAttemptResponse attempt(UUID id) {
        return ownedAttempt(id, currentUser.id());
    }

    @Transactional(readOnly = true)
    public List<PaymentAttemptResponse> attempts(UUID drawdownId) {
        ownedDrawdown(drawdownId);
        return attemptRepository.findAllByInstalmentDrawdownIdOrderByRequestedAtDesc(drawdownId).stream()
                .map(PaymentAttemptResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RepaymentHistoryResponse> history(UUID drawdownId) {
        ownedDrawdown(drawdownId);
        return repaymentRepository.findAllByDrawdownId(drawdownId).stream()
                .map(RepaymentHistoryResponse::from).toList();
    }

    private PaymentAttemptResponse ownedAttempt(UUID id, UUID userId) {
        PaymentAttempt attempt = attemptRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Payment attempt not found"));
        if (!attempt.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Payment attempt not found");
        }
        return PaymentAttemptResponse.from(attempt);
    }

    private void ownedDrawdown(UUID id) {
        CreditDrawdown drawdown = drawdownRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Drawdown not found"));
        if (!drawdown.getAccount().getUser().getId().equals(currentUser.id())) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Drawdown not found");
        }
    }
}
