ALTER TABLE users ADD COLUMN email VARCHAR(255);
ALTER TABLE users ADD COLUMN password_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('CUSTOMER', 'ADMIN'));
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email)) WHERE email IS NOT NULL;

ALTER TABLE idempotency_records ADD COLUMN user_id UUID;
UPDATE idempotency_records SET user_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE idempotency_records ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE idempotency_records ADD CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE idempotency_records DROP CONSTRAINT uq_idempotency_operation_key;
ALTER TABLE idempotency_records ADD CONSTRAINT uq_idempotency_user_operation_key
    UNIQUE (user_id, operation, idempotency_key);
