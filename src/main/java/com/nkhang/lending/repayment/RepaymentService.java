package com.nkhang.lending.repayment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.common.idempotency.IdempotencyOperation;
import com.nkhang.lending.common.idempotency.IdempotencyRecord;
import com.nkhang.lending.common.idempotency.IdempotencyService;
import com.nkhang.lending.common.idempotency.IdempotencyStatus;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
import com.nkhang.lending.drawdown.CreditDrawdown;
import com.nkhang.lending.drawdown.CreditDrawdownRepository;
import com.nkhang.lending.drawdown.DrawdownStatus;
import com.nkhang.lending.instalment.Instalment;
import com.nkhang.lending.instalment.InstalmentRepository;
import com.nkhang.lending.instalment.InstalmentStatus;
import com.nkhang.lending.repayment.dto.RepaymentHistoryResponse;
import com.nkhang.lending.repayment.dto.RepaymentResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import com.nkhang.lending.config.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepaymentService {
    final CreditAccountRepository accountRepository;
    final CreditDrawdownRepository drawdownRepository;
    final InstalmentRepository instalmentRepository;
    final RepaymentTransactionRepository repaymentRepository;
    final IdempotencyService idempotencyService;
    final ObjectMapper objectMapper;
    final Clock clock;

    final CurrentUser currentUser;

    @Transactional
    public RepaymentResponse repay(UUID instalmentId, String key) {
        IdempotencyRecord record = idempotencyService.begin(
                IdempotencyOperation.REPAY_INSTALMENT, currentUser.id(), key, instalmentId.toString());
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return readResponse(record.getResponseBody());
        }

        // All balance changes use account -> drawdown -> instalment lock order.
        CreditAccount account = accountRepository.findByUserIdForUpdate(currentUser.id())
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
        Instalment earliest = instalmentRepository.findFirstByDrawdownIdAndStatusOrderBySequenceNoAsc(
                        drawdown.getId(), InstalmentStatus.UNPAID)
                .orElseThrow(() -> new AppException(ErrorCode.DRAWDOWN_NOT_ACTIVE));
        if (!earliest.getId().equals(instalmentId)) {
            throw new AppException(ErrorCode.INSTALMENT_OUT_OF_ORDER);
        }

        Instant now = clock.instant();
        instalment.markPaid(now);
        drawdown.repay(instalment.getAmount());
        account.repay(instalment.getAmount());
        RepaymentTransaction transaction = repaymentRepository.save(RepaymentTransaction.success(instalment, now));
        RepaymentResponse response = new RepaymentResponse(transaction.getId(), instalment.getId(),
                drawdown.getId(), transaction.getAmount(), drawdown.getOutstandingPrincipal(),
                account.getUsedLimit(), account.getAvailableLimit(), instalment.getStatus().name(),
                drawdown.getStatus().name(), now);
        idempotencyService.complete(record, transaction.getId(), writeResponse(response), 200);
        return response;
    }

    @Transactional(readOnly = true)
    public List<RepaymentHistoryResponse> history(UUID drawdownId) {
        CreditDrawdown drawdown = drawdownRepository.findById(drawdownId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Drawdown not found"));
        if (!drawdown.getAccount().getUser().getId().equals(currentUser.id())) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Drawdown not found");
        }
        return repaymentRepository.findAllByDrawdownId(drawdownId).stream()
                .map(RepaymentHistoryResponse::from).toList();
    }

    private String writeResponse(RepaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize repayment response", exception);
        }
    }

    private RepaymentResponse readResponse(String body) {
        try {
            return objectMapper.readValue(body, RepaymentResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read repayment response", exception);
        }
    }
}
