package com.nkhang.lending.instalment;

import lombok.RequiredArgsConstructor;
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

    @Transactional
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public int markOverdue() {
        return instalments.markOverdue(LocalDate.now(clock));
    }
}
