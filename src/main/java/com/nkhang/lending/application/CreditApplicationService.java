package com.nkhang.lending.application;

import com.nkhang.lending.assessment.CreditAssessmentPolicy;
import com.nkhang.lending.assessment.CreditDecision;
import com.nkhang.lending.assessment.DecisionStatus;
import com.nkhang.lending.application.dto.CreditApplicationRequest;
import com.nkhang.lending.application.dto.CreditApplicationResponse;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import com.nkhang.lending.credit.CreditAccount;
import com.nkhang.lending.credit.CreditAccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditApplicationService {
    final DemoUserRepository userRepository;
    final CreditApplicationRepository applicationRepository;
    final CreditAccountRepository accountRepository;
    final CreditAssessmentPolicy policy;
    final Clock clock;

    @Value("${lending.demo-user-id}")
    UUID demoUserId;

    @Transactional
    public CreditApplicationResponse submit(CreditApplicationRequest request) {
        if (request.currency() != null && !"SGD".equals(request.currency())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Only SGD is supported");
        }
        DemoUser user = userRepository.findByIdForUpdate(demoUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Demo user not found"));
        if (accountRepository.existsByUserId(demoUserId)) {
            throw new AppException(ErrorCode.APPLICATION_ALREADY_APPROVED);
        }
        CreditDecision decision = policy.assess(request.monthlyNetIncome(),
                request.monthlyLivingExpenses(), request.monthlyDebtPayments());
        CreditApplication application = applicationRepository.save(
                CreditApplication.assessed(user, request, decision, clock.instant()));
        if (decision.status() == DecisionStatus.APPROVED) {
            accountRepository.save(CreditAccount.active(user, decision.approvedLimit()));
        }
        return CreditApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public CreditApplicationResponse latest() {
        return applicationRepository.findFirstByUserIdOrderByAssessedAtDesc(demoUserId)
                .map(CreditApplicationResponse::from)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "No credit application yet"));
    }
}
