CREATE TABLE IF NOT EXISTS customer (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    external_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    profile VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(255),
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_by VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_customer_merchant ON customer(merchant_id);
CREATE INDEX IF NOT EXISTS idx_customer_status ON customer(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_customer_external_id ON customer(merchant_id, external_id);

CREATE TABLE IF NOT EXISTS account_metadata (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    merchant_id UUID NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    value VARCHAR(1024) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(255),
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_by VARCHAR(255),
    UNIQUE(account_id, display_name)
);
CREATE INDEX IF NOT EXISTS idx_account_metadata_account ON account_metadata(account_id);
