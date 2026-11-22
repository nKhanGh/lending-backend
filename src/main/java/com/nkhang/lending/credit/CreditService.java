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

    final CreditAccountRepository creditAccountRepository;

    @Value("${lending.demo-user-id}")
    UUID demoUserId;

    @Transactional(readOnly = true)
    public CreditAccountResponse getDemoAccount() {
        CreditAccount account = creditAccountRepository.findByUserId(demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Credit account not found"));
        return CreditAccountResponse.from(account);
    }
}

