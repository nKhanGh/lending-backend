package com.nkhang.lending.drawdown;

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
import com.nkhang.lending.drawdown.dto.DrawdownDetailResponse;
import com.nkhang.lending.drawdown.dto.DrawdownRequest;
import com.nkhang.lending.drawdown.dto.DrawdownResponse;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditDrawdownService {
    final CreditDrawdownRepository drawdownRepository;
    final CreditAccountRepository accountRepository;
    final InstalmentRepository instalmentRepository;
    final InstalmentScheduleFactory scheduleFactory;
    final IdempotencyService idempotencyService;
    final ObjectMapper objectMapper;
    final Clock clock;

    @Value("${lending.demo-user-id}")
    UUID demoUserId;

    @Transactional
    public DrawdownDetailResponse create(DrawdownRequest request, String key) {
        BigDecimal amount = request.amount();
        if (amount == null || amount.scale() > 2 || amount.compareTo(new BigDecimal("0.04")) < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Amount must be at least SGD 0.04 with two decimals");
        }
        amount = amount.setScale(2);
        IdempotencyRecord record = idempotencyService.begin(
                IdempotencyOperation.CREATE_DRAWDOWN, key, amount.toPlainString());
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return readResponse(record.getResponseBody());
        }

        CreditAccount account = accountRepository.findByUserIdForUpdate(demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Submit a credit application first"));
        account.utilize(amount);
        Instant now = clock.instant();
        CreditDrawdown drawdown = drawdownRepository.save(CreditDrawdown.active(account, amount, now));
        List<Instalment> schedule = instalmentRepository.saveAll(
                scheduleFactory.create(drawdown, LocalDate.ofInstant(now, clock.getZone())));
        DrawdownDetailResponse response = DrawdownDetailResponse.from(drawdown, schedule);
        idempotencyService.complete(record, drawdown.getId(), writeResponse(response), 201);
        return response;
    }

    @Transactional(readOnly = true)
    public List<DrawdownResponse> list() {
        return drawdownRepository.findAllForUser(demoUserId).stream()
                .map(DrawdownResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DrawdownDetailResponse detail(UUID id) {
        CreditDrawdown drawdown = drawdownRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Drawdown not found"));
        if (!drawdown.getAccount().getUser().getId().equals(demoUserId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Drawdown not found");
        }
        return DrawdownDetailResponse.from(drawdown,
                instalmentRepository.findAllByDrawdownIdOrderBySequenceNoAsc(id));
    }

    private String writeResponse(DrawdownDetailResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize drawdown response", exception);
        }
    }

    private DrawdownDetailResponse readResponse(String body) {
        try {
            return objectMapper.readValue(body, DrawdownDetailResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read drawdown response", exception);
        }
    }
}
