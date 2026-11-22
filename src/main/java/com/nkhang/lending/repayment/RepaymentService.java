package com.nkhang.lending.repayment;

import com.nkhang.lending.checkout.OrderStatus;
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
import com.nkhang.lending.instalment.InstalmentStatus;
import com.nkhang.lending.repayment.dto.RepaymentHistoryResponse;
import com.nkhang.lending.repayment.dto.RepaymentResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
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

    final InstalmentRepository instalmentRepository;
    final RepaymentRepository repaymentRepository;
    final CreditAccountRepository creditAccountRepository;
    final IdempotencyService idempotencyService;
    final Clock clock;

    @Value("${lending.demo-user-id}")
    UUID demoUserId;

    @Transactional
    public RepaymentResponse pay(UUID instalmentId, String idempotencyKey) {
        IdempotencyRecord record = idempotencyService.begin(
                IdempotencyOperation.PAY_INSTALMENT, idempotencyKey, instalmentId.toString());
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return replay(record);
        }

        Instalment instalment = instalmentRepository.findByIdForUpdate(instalmentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Instalment not found"));
        if (!instalment.getOrder().getUserId().equals(demoUserId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Instalment not found");
        }
        if (instalment.getOrder().getStatus() != OrderStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_CHECKOUT_STATE, "Order is not confirmed");
        }
        if (instalment.getStatus() == InstalmentStatus.PAID) {
            throw new AppException(ErrorCode.INSTALMENT_ALREADY_PAID);
        }

        CreditAccount account = findAccountForUpdate();
        Instant now = clock.instant();
        instalment.markPaid(now);
        account.repay(instalment.getAmount());
        Repayment repayment = repaymentRepository.save(Repayment.create(instalment, now));

        idempotencyService.complete(record, repayment.getId(), 200);
        return RepaymentResponse.from(repayment, account);
    }

    @Transactional(readOnly = true)
    public List<RepaymentHistoryResponse> getHistory() {
        return repaymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(repayment -> repayment.getInstalment().getOrder().getUserId().equals(demoUserId))
                .map(RepaymentHistoryResponse::from)
                .toList();
    }

    private RepaymentResponse replay(IdempotencyRecord record) {
        Repayment repayment = repaymentRepository.findDetailedById(record.getResourceId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Repayment not found"));
        CreditAccount account = creditAccountRepository.findByUserId(demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
        return RepaymentResponse.from(repayment, account);
    }

    private CreditAccount findAccountForUpdate() {
        return creditAccountRepository.findByUserIdForUpdate(demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
    }
}

