ALTER TABLE instalments DROP CONSTRAINT instalments_status_check;
ALTER TABLE instalments ADD CONSTRAINT instalments_status_check
    CHECK (status IN ('UNPAID', 'OVERDUE', 'PAID'));
CREATE INDEX idx_instalments_due_unpaid ON instalments(due_date) WHERE status = 'UNPAID';
