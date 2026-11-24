CREATE TABLE ledger_journals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    operation VARCHAR(30) NOT NULL CHECK (operation IN ('DRAWDOWN', 'REPAYMENT')),
    source_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ledger_source UNIQUE (operation, source_id)
);
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    journal_id UUID NOT NULL REFERENCES ledger_journals(id),
    account_code VARCHAR(40) NOT NULL CHECK
        (account_code IN ('CUSTOMER_RECEIVABLE', 'SIMULATED_FUNDING', 'SIMULATED_CASH')),
    direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0)
);
CREATE INDEX idx_ledger_journals_user_time ON ledger_journals(user_id, created_at DESC);
CREATE INDEX idx_ledger_entries_journal ON ledger_entries(journal_id);

-- Preserve reconciliation for drawdowns and repayments created before V7.
INSERT INTO ledger_journals (id, user_id, operation, source_id, created_at)
SELECT gen_random_uuid(), a.user_id, 'DRAWDOWN', d.id, d.created_at
FROM credit_drawdowns d JOIN credit_accounts a ON a.id = d.account_id;
INSERT INTO ledger_entries (id, journal_id, account_code, direction, amount)
SELECT gen_random_uuid(), j.id, 'CUSTOMER_RECEIVABLE', 'DEBIT', d.original_amount
FROM ledger_journals j JOIN credit_drawdowns d ON j.source_id = d.id WHERE j.operation = 'DRAWDOWN';
INSERT INTO ledger_entries (id, journal_id, account_code, direction, amount)
SELECT gen_random_uuid(), j.id, 'SIMULATED_FUNDING', 'CREDIT', d.original_amount
FROM ledger_journals j JOIN credit_drawdowns d ON j.source_id = d.id WHERE j.operation = 'DRAWDOWN';

INSERT INTO ledger_journals (id, user_id, operation, source_id, created_at)
SELECT gen_random_uuid(), a.user_id, 'REPAYMENT', r.id, r.paid_at
FROM repayment_transactions r
JOIN instalments i ON i.id = r.instalment_id
JOIN credit_drawdowns d ON d.id = i.drawdown_id
JOIN credit_accounts a ON a.id = d.account_id;
INSERT INTO ledger_entries (id, journal_id, account_code, direction, amount)
SELECT gen_random_uuid(), j.id, 'SIMULATED_CASH', 'DEBIT', r.amount
FROM ledger_journals j JOIN repayment_transactions r ON j.source_id = r.id WHERE j.operation = 'REPAYMENT';
INSERT INTO ledger_entries (id, journal_id, account_code, direction, amount)
SELECT gen_random_uuid(), j.id, 'CUSTOMER_RECEIVABLE', 'CREDIT', r.amount
FROM ledger_journals j JOIN repayment_transactions r ON j.source_id = r.id WHERE j.operation = 'REPAYMENT';
