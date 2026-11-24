CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    instalment_id UUID NOT NULL REFERENCES instalments(id),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    requested_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    provider_event_id VARCHAR(120) UNIQUE,
    CONSTRAINT payment_attempt_resolution CHECK ((status = 'PENDING') = (resolved_at IS NULL))
);
CREATE UNIQUE INDEX uq_pending_payment_per_instalment ON payment_attempts(instalment_id) WHERE status = 'PENDING';
CREATE INDEX idx_payment_attempts_user_time ON payment_attempts(user_id, requested_at DESC);
