CREATE TABLE credit_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    verified_monthly_income NUMERIC(19,2) CHECK (verified_monthly_income >= 0),
    late_payments_12m INTEGER NOT NULL DEFAULT 0 CHECK (late_payments_12m BETWEEN 0 AND 12),
    updated_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE credit_applications ADD COLUMN verified_monthly_income NUMERIC(19,2);
ALTER TABLE credit_applications ADD COLUMN late_payments_12m INTEGER NOT NULL DEFAULT 0;
