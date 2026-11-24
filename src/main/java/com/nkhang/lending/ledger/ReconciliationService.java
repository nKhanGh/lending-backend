package com.nkhang.lending.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationService {
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<ReconciliationResponse> accounts() {
        return jdbc.query("""
                select a.user_id, a.used_limit,
                    coalesce((select sum(d.outstanding_principal) from credit_drawdowns d
                              where d.account_id = a.id), 0) as drawdown_outstanding,
                    coalesce((select sum(case when e.direction = 'DEBIT' then e.amount else -e.amount end)
                              from ledger_entries e join ledger_journals j on j.id = e.journal_id
                              where j.user_id = a.user_id and e.account_code = 'CUSTOMER_RECEIVABLE'), 0)
                              as ledger_outstanding,
                    (select count(*) from ledger_journals j where j.user_id = a.user_id
                     and coalesce((select sum(case when e.direction = 'DEBIT' then e.amount else -e.amount end)
                                   from ledger_entries e where e.journal_id = j.id), 0) <> 0)
                              as unbalanced_journals
                from credit_accounts a order by a.user_id
                """, (rs, row) -> {
            BigDecimal used = rs.getBigDecimal("used_limit");
            BigDecimal drawdowns = rs.getBigDecimal("drawdown_outstanding");
            BigDecimal ledger = rs.getBigDecimal("ledger_outstanding");
            long unbalanced = rs.getLong("unbalanced_journals");
            return new ReconciliationResponse(rs.getObject("user_id", java.util.UUID.class),
                    used, drawdowns, ledger, unbalanced,
                    used.compareTo(drawdowns) == 0 && used.compareTo(ledger) == 0 && unbalanced == 0);
        });
    }
}
