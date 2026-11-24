package com.nkhang.lending.credit;

import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.credit.dto.CreditAccountResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditService {
    final CreditAccountRepository accountRepository;

    @Value("${lending.demo-user-id}")
    UUID demoUserId;

    @Transactional(readOnly = true)
    public CreditAccountResponse getAccount() {
        return accountRepository.findByUserId(demoUserId)
                .map(CreditAccountResponse::from)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "No credit account yet"));
    }
}
