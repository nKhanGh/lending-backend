ALTER TABLE instalments ADD COLUMN paid_amount NUMERIC(19,2) NOT NULL DEFAULT 0;
ALTER TABLE instalments ADD CONSTRAINT instalment_paid_amount_valid
    CHECK (paid_amount >= 0 AND paid_amount <= amount);
UPDATE instalments SET paid_amount = amount WHERE status = 'PAID';
ALTER TABLE instalments ADD CONSTRAINT instalment_paid_status_valid
    CHECK ((status = 'PAID') = (paid_amount = amount));
ALTER TABLE repayment_transactions DROP CONSTRAINT repayment_transactions_instalment_id_key;
CREATE INDEX idx_repayments_instalment ON repayment_transactions(instalment_id);
