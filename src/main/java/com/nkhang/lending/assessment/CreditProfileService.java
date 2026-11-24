package com.nkhang.lending.assessment;

import com.nkhang.lending.application.LendingUserRepository;
import com.nkhang.lending.audit.AuditService;
import com.nkhang.lending.config.CurrentUser;
import com.nkhang.lending.assessment.dto.CreditProfileRequest;
import com.nkhang.lending.assessment.dto.CreditProfileResponse;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditProfileService {
    private final CreditProfileRepository profiles;
    private final LendingUserRepository users;
    private final Clock clock;
    private final AuditService audit;
    private final CurrentUser currentUser;

    @Transactional
    public CreditProfileResponse upsert(UUID userId, CreditProfileRequest request) {
        if (!users.existsById(userId)) throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found");
        CreditProfile profile = profiles.findById(userId).orElseGet(() ->
                CreditProfile.of(userId, request.verifiedMonthlyIncome(), request.latePayments12m(), clock.instant()));
        profile.update(request.verifiedMonthlyIncome(), request.latePayments12m(), clock.instant());
        CreditProfile saved = profiles.save(profile);
        audit.record(currentUser.id(), userId, "CREDIT_PROFILE_UPDATED", "CREDIT_PROFILE",
                userId, "latePayments12m=" + request.latePayments12m());
        return CreditProfileResponse.from(saved);
    }
}
