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
import com.nkhang.lending.config.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditApplicationService {
    final LendingUserRepository userRepository;
    final CreditApplicationRepository applicationRepository;
    final CreditAccountRepository accountRepository;
    final CreditAssessmentPolicy policy;
    final Clock clock;

    final CurrentUser currentUser;

    @Transactional
    public CreditApplicationResponse submit(CreditApplicationRequest request) {
        if (request.currency() != null && !"SGD".equals(request.currency())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Only SGD is supported");
        }
        LendingUser user = userRepository.findByIdForUpdate(currentUser.id())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Demo user not found"));
        if (accountRepository.existsByUserId(currentUser.id())) {
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
        return applicationRepository.findFirstByUserIdOrderByAssessedAtDesc(currentUser.id())
                .map(CreditApplicationResponse::from)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "No credit application yet"));
    }
}
