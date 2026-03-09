CREATE TABLE IF NOT EXISTS api_key (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    client_id UUID NOT NULL REFERENCES oauth_registered_client(id),
    name VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(12) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    secret_hash VARCHAR(255) NOT NULL,
    environment VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    scopes TEXT,
    ip_allowlist TEXT,
    signing_key_encrypted TEXT,
    last_used_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_api_key_client ON api_key(client_id);
CREATE INDEX IF NOT EXISTS idx_api_key_prefix ON api_key(key_prefix);
CREATE INDEX IF NOT EXISTS idx_api_key_hash ON api_key(key_hash);
CREATE INDEX IF NOT EXISTS idx_api_key_status ON api_key(client_id, status);

ALTER TABLE oauth_registered_client ADD COLUMN IF NOT EXISTS ip_allowlist TEXT;

CREATE TABLE IF NOT EXISTS webhook_config (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    client_id UUID NOT NULL REFERENCES oauth_registered_client(id),
    name VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    secret_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    event_types TEXT NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_config_client ON webhook_config(client_id);
CREATE INDEX IF NOT EXISTS idx_webhook_config_status ON webhook_config(client_id, status);
