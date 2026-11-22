CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    price NUMERIC(18, 2) NOT NULL CHECK (price > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'SGD'),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE credit_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    total_limit NUMERIC(18, 2) NOT NULL CHECK (total_limit > 0),
    outstanding_amount NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (outstanding_amount >= 0),
    reserved_amount NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (reserved_amount >= 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'SGD'),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_credit_within_limit
        CHECK (outstanding_amount + reserved_amount <= total_limit)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(18, 2) NOT NULL CHECK (unit_price > 0),
    total_amount NUMERIC(18, 2) NOT NULL CHECK (total_amount > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'SGD'),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ
);

CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);

CREATE TABLE credit_reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id),
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CAPTURED', 'RELEASED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reservations_expiry ON credit_reservations(status, expires_at);

CREATE TABLE instalments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    sequence_no SMALLINT NOT NULL CHECK (sequence_no BETWEEN 1 AND 4),
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('UNPAID', 'PAID')),
    paid_at TIMESTAMPTZ,
    CONSTRAINT uk_instalment_order_sequence UNIQUE (order_id, sequence_no)
);

CREATE INDEX idx_instalments_order ON instalments(order_id, sequence_no);

CREATE TABLE repayments (
    id UUID PRIMARY KEY,
    instalment_id UUID NOT NULL UNIQUE REFERENCES instalments(id),
    amount NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    operation VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    resource_id UUID,
    response_body TEXT,
    http_status INTEGER,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_idempotency_operation_key UNIQUE (operation, idempotency_key)
);

