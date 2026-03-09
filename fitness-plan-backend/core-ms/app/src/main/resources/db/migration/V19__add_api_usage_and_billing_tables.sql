CREATE TABLE IF NOT EXISTS api_key_usage_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_id UUID NOT NULL,
    key_prefix VARCHAR(12),
    endpoint VARCHAR(512) NOT NULL,
    method VARCHAR(10) NOT NULL,
    status_code INT NOT NULL,
    response_time_ms INT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ,
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_api_usage_merchant ON api_key_usage_log(merchant_id);
CREATE INDEX IF NOT EXISTS idx_api_usage_created ON api_key_usage_log(merchant_id, created_at);

CREATE TABLE IF NOT EXISTS webhook_delivery (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_id UUID NOT NULL,
    webhook_public_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ,
    last_status_code INT,
    last_error TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ,
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_webhook_delivery_merchant ON webhook_delivery(merchant_id);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_status ON webhook_delivery(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_webhook ON webhook_delivery(webhook_public_id);

CREATE TABLE IF NOT EXISTS billing_plan (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    merchant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    platform_fee_cents BIGINT NOT NULL DEFAULT 0,
    per_account_fee_cents BIGINT NOT NULL DEFAULT 0,
    per_transaction_fee_cents BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ,
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_billing_plan_merchant ON billing_plan(merchant_id);
CREATE INDEX IF NOT EXISTS idx_billing_plan_status ON billing_plan(merchant_id, status);

CREATE TABLE IF NOT EXISTS billing_usage_snapshot (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    active_account_count INT NOT NULL DEFAULT 0,
    transaction_count INT NOT NULL DEFAULT 0,
    api_call_count INT NOT NULL DEFAULT 0,
    export_count INT NOT NULL DEFAULT 0,
    webhook_delivery_count INT NOT NULL DEFAULT 0,
    storage_bytes BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ,
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_billing_usage_uniq ON billing_usage_snapshot(merchant_id, snapshot_date);

CREATE TABLE IF NOT EXISTS billing_invoice (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    merchant_id UUID NOT NULL,
    billing_plan_id BIGINT NOT NULL REFERENCES billing_plan(id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    platform_fee_cents BIGINT NOT NULL DEFAULT 0,
    account_fee_cents BIGINT NOT NULL DEFAULT 0,
    transaction_fee_cents BIGINT NOT NULL DEFAULT 0,
    total_cents BIGINT NOT NULL DEFAULT 0,
    account_count INT NOT NULL DEFAULT 0,
    transaction_count INT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    issued_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ,
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_billing_invoice_merchant ON billing_invoice(merchant_id);
CREATE INDEX IF NOT EXISTS idx_billing_invoice_status ON billing_invoice(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_billing_invoice_period ON billing_invoice(merchant_id, period_start, period_end);
