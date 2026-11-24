package com.nkhang.lending.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public void drawdown(UUID userId, UUID drawdownId, BigDecimal amount) {
        UUID journalId = journal(userId, "DRAWDOWN", drawdownId);
        entry(journalId, "CUSTOMER_RECEIVABLE", "DEBIT", amount);
        entry(journalId, "SIMULATED_FUNDING", "CREDIT", amount);
    }

    public void repayment(UUID userId, UUID repaymentId, BigDecimal amount) {
        UUID journalId = journal(userId, "REPAYMENT", repaymentId);
        entry(journalId, "SIMULATED_CASH", "DEBIT", amount);
        entry(journalId, "CUSTOMER_RECEIVABLE", "CREDIT", amount);
    }

    private UUID journal(UUID userId, String operation, UUID sourceId) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into ledger_journals(id,user_id,operation,source_id,created_at) values (?,?,?,?,?)",
                id, userId, operation, sourceId, Timestamp.from(clock.instant()));
        return id;
    }

    private void entry(UUID journalId, String account, String direction, BigDecimal amount) {
        jdbc.update("insert into ledger_entries(id,journal_id,account_code,direction,amount) values (?,?,?,?,?)",
                UUID.randomUUID(), journalId, account, direction, amount);
    }
}
