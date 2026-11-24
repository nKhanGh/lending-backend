CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id),
    subject_user_id UUID REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id UUID,
    details VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_subject_time ON audit_events(subject_user_id, occurred_at DESC);
CREATE INDEX idx_audit_time ON audit_events(occurred_at DESC);
