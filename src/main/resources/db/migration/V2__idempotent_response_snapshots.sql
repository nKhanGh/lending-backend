-- Existing V1 rows remain readable. New operations write snapshots for exact retries.
ALTER TABLE credit_drawdowns
    ADD COLUMN available_limit_after NUMERIC(19, 2) CHECK (available_limit_after >= 0);

ALTER TABLE repayment_transactions
    ADD COLUMN remaining_principal_after NUMERIC(19, 2) CHECK (remaining_principal_after >= 0),
    ADD COLUMN available_limit_after NUMERIC(19, 2) CHECK (available_limit_after >= 0);
