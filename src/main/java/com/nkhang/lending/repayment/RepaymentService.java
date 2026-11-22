package com.nkhang.lending.repayment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkhang.lending.checkout.Order;
import com.nkhang.lending.checkout.OrderRepository;
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
import com.nkhang.lending.repayment.dto.SettlementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.math.BigDecimal;
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
    final OrderRepository orderRepository;
    final ObjectMapper objectMapper;

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

    @Transactional
    public SettlementResponse settleOrder(UUID orderId, String idempotencyKey) {
        IdempotencyRecord record = idempotencyService.begin(
                IdempotencyOperation.SETTLE_ORDER, idempotencyKey, orderId.toString());
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return readSettlement(record.getResponseBody());
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(demoUserId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_CHECKOUT_STATE, "Order is not confirmed");
        }

        List<Instalment> unpaid = instalmentRepository.findAllByOrderIdForUpdate(orderId).stream()
                .filter(instalment -> instalment.getStatus() == InstalmentStatus.UNPAID)
                .toList();
        if (unpaid.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_ALREADY_SETTLED);
        }

        CreditAccount account = findAccountForUpdate();
        Instant now = clock.instant();
        BigDecimal total = BigDecimal.ZERO;
        List<UUID> repaymentIds = new java.util.ArrayList<>();
        for (Instalment instalment : unpaid) {
            instalment.markPaid(now);
            Repayment repayment = repaymentRepository.save(Repayment.create(instalment, now));
            repaymentIds.add(repayment.getId());
            total = total.add(instalment.getAmount());
        }
        account.repay(total);
        SettlementResponse response = new SettlementResponse(
                orderId, List.copyOf(repaymentIds), unpaid.size(), total,
                account.getOutstandingAmount(), account.getAvailableCredit(), account.getCurrency());
        idempotencyService.complete(record, orderId, writeSettlement(response), 200);
        return response;
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

    private String writeSettlement(SettlementResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize settlement response", exception);
        }
    }

    private SettlementResponse readSettlement(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, SettlementResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read settlement response", exception);
        }
    }
}

