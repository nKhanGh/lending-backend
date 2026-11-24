package com.nkhang.lending.instalment;

import lombok.RequiredArgsConstructor;
import com.nkhang.lending.audit.AuditService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OverdueService {
    private final InstalmentRepository instalments;
    private final Clock clock;
    private final AuditService audit;

    @Transactional
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public int markOverdue() {
        int count = instalments.markOverdue(LocalDate.now(clock));
        if (count > 0) audit.record(null, null, "OVERDUE_BATCH", "INSTALMENT", null, "count=" + count);
        return count;
    }
}
