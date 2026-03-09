-- =====================================================
-- Identity MS Schema - Squashed Migration
-- =====================================================

-- Organization table
CREATE TABLE IF NOT EXISTS organization (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    plan VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    database_name VARCHAR(255) UNIQUE,
    database_created BOOLEAN NOT NULL DEFAULT FALSE,
    max_knowledge_bases INTEGER NOT NULL DEFAULT 5,
    settings JSONB NOT NULL DEFAULT '{}',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

-- OAuth User table
CREATE TABLE IF NOT EXISTS oauth_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    first_name VARCHAR(100),
    middle_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    merchant_id UUID,
    organization_id UUID REFERENCES organization(id),
    user_type VARCHAR(20),
    trust_level VARCHAR(20),
    email_verified BOOLEAN NOT NULL DEFAULT false,
    phone_number_verified BOOLEAN NOT NULL DEFAULT false,
    invitation_status BOOLEAN NOT NULL DEFAULT false,
    registration_complete BOOLEAN NOT NULL DEFAULT false,
    date_of_birth DATE,
    tax_identification_number VARCHAR(50),
    locale VARCHAR(10) NOT NULL DEFAULT 'en',
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_failed_login TIMESTAMPTZ,
    environment_preference VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    environment_last_switched_at TIMESTAMPTZ,
    registration_source VARCHAR(50) DEFAULT 'INVITATION',
    picture_url VARCHAR(1024),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

CREATE TABLE IF NOT EXISTS oauth_user_authority (
    user_id UUID NOT NULL,
    authority VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, authority),
    CONSTRAINT fk_user_authorities_user FOREIGN KEY (user_id) REFERENCES oauth_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_provider_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    linked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_oauth_provider_user FOREIGN KEY (user_id) REFERENCES oauth_user(id) ON DELETE CASCADE,
    CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id)
);

-- OAuth Registered Client table (id is UUID)
CREATE TABLE IF NOT EXISTS oauth_registered_client (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_id_issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sandbox_client_secret VARCHAR(255),
    sandbox_client_secret_expires_at TIMESTAMPTZ,
    production_client_secret VARCHAR(255),
    production_client_secret_expires_at TIMESTAMPTZ,
    client_name VARCHAR(200) NOT NULL,
    failed_auth_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_failed_auth TIMESTAMPTZ,
    environment_mode VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    domain VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    plan VARCHAR(50) DEFAULT 'TRIAL',
    organization_id UUID REFERENCES organization(id),
    knowledge_base_id VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

-- OAuth lookup tables
CREATE TABLE IF NOT EXISTS oauth_scope (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS oauth_authentication_method (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS oauth_grant_type (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Client relationship tables (registered_client_id references oauth_registered_client.id)
CREATE TABLE IF NOT EXISTS oauth_client_scope (
    registered_client_id UUID NOT NULL,
    scope_id INT NOT NULL,
    PRIMARY KEY (registered_client_id, scope_id),
    CONSTRAINT fk_client_scope_client FOREIGN KEY (registered_client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_scope_scope FOREIGN KEY (scope_id) REFERENCES oauth_scope(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_authentication_method (
    registered_client_id UUID NOT NULL,
    method_id INT NOT NULL,
    PRIMARY KEY (registered_client_id, method_id),
    CONSTRAINT fk_client_method_client FOREIGN KEY (registered_client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_method_method FOREIGN KEY (method_id) REFERENCES oauth_authentication_method(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_grant_type (
    registered_client_id UUID NOT NULL,
    grant_type_id INT NOT NULL,
    PRIMARY KEY (registered_client_id, grant_type_id),
    CONSTRAINT fk_client_grant_client FOREIGN KEY (registered_client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_grant_type FOREIGN KEY (grant_type_id) REFERENCES oauth_grant_type(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_redirect_uri (
    registered_client_id UUID NOT NULL,
    uri VARCHAR(2000) NOT NULL,
    PRIMARY KEY (registered_client_id, uri),
    CONSTRAINT fk_redirect_uri_client FOREIGN KEY (registered_client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_post_logout_redirect_uri (
    registered_client_id UUID NOT NULL,
    uri VARCHAR(2000) NOT NULL,
    PRIMARY KEY (registered_client_id, uri),
    CONSTRAINT fk_post_logout_uri_client FOREIGN KEY (registered_client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_setting (
    registered_client_id UUID NOT NULL,
    setting_name VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    PRIMARY KEY (registered_client_id, setting_name),
    CONSTRAINT fk_client_setting_client FOREIGN KEY (registered_client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_client_token_setting (
    registered_client_id UUID NOT NULL,
    setting_name VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    PRIMARY KEY (registered_client_id, setting_name),
    CONSTRAINT fk_token_setting_client FOREIGN KEY (registered_client_id) REFERENCES oauth_registered_client(id) ON DELETE CASCADE
);

-- Refresh token table
CREATE TABLE IF NOT EXISTS refresh_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jti VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    token_hash VARCHAR(500) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_fingerprint VARCHAR(255),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_jti VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_refresh_token_user_id FOREIGN KEY (user_id) REFERENCES oauth_user(id) ON DELETE CASCADE
);

-- Trusted device table
CREATE TABLE IF NOT EXISTS trusted_device (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    device_fingerprint VARCHAR(255) NOT NULL,
    device_fingerprint_hash VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    trusted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    trust_count INTEGER DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_trusted_device_user FOREIGN KEY (user_id) REFERENCES oauth_user(id) ON DELETE CASCADE
);

-- Process tables
CREATE TABLE IF NOT EXISTS process (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    state VARCHAR(255) NOT NULL,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    expiry TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    external_reference VARCHAR(255),
    channel VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT unique_process_type_external_reference UNIQUE (type, external_reference)
);

CREATE TABLE IF NOT EXISTS process_request (
    id BIGSERIAL PRIMARY KEY,
    process_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    state VARCHAR(255) NOT NULL,
    channel VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_process_request_process FOREIGN KEY (process_id) REFERENCES process (id)
);

CREATE TABLE IF NOT EXISTS process_request_data (
    process_request_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    value TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    PRIMARY KEY (process_request_id, name),
    CONSTRAINT fk_process_request_data FOREIGN KEY (process_request_id) REFERENCES process_request (id)
);

CREATE TABLE IF NOT EXISTS process_request_stakeholder (
    id BIGSERIAL PRIMARY KEY,
    process_request_id BIGINT NOT NULL,
    stakeholder_id VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_process_request_stakeholder FOREIGN KEY (process_request_id) REFERENCES process_request (id)
);

CREATE TABLE IF NOT EXISTS process_event_transition (
    id BIGSERIAL PRIMARY KEY,
    process_id BIGINT NOT NULL,
    event VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    old_state VARCHAR(255) NOT NULL,
    new_state VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_process_event_transition FOREIGN KEY (process_id) REFERENCES process (id)
);

-- Rate limit config table
CREATE TABLE IF NOT EXISTS rate_limit_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    method_name VARCHAR(100) NOT NULL,
    subscription_tier VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    scope VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    capacity INT NOT NULL,
    time_value INT NOT NULL,
    time_unit VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_by VARCHAR(255) DEFAULT 'system',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT rate_limit_config_method_scope_tier_key UNIQUE (method_name, scope, subscription_tier)
);

-- Country table
CREATE TABLE IF NOT EXISTS country (
    id SERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    iso2_code VARCHAR(2) NOT NULL,
    iso3_code VARCHAR(3) NOT NULL,
    numeric_code VARCHAR(3) NOT NULL,
    dial_code VARCHAR(10) NOT NULL,
    flag_url VARCHAR(255) NOT NULL,
    region VARCHAR(50) DEFAULT '',
    sub_region VARCHAR(100) DEFAULT '',
    default_currency_code VARCHAR(3) DEFAULT 'USD',
    enabled BOOLEAN DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255) DEFAULT 'system',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(iso2_code),
    UNIQUE(iso3_code),
    UNIQUE(public_id)
);

-- Currency table
CREATE TABLE IF NOT EXISTS currency (
    id SERIAL PRIMARY KEY,
    public_id UUID DEFAULT gen_random_uuid(),
    name VARCHAR(100),
    code VARCHAR(10) UNIQUE,
    symbol VARCHAR(10),
    symbol_native VARCHAR(10),
    iso_num INT,
    iso_digits INT DEFAULT 2,
    decimals INT DEFAULT 2,
    type VARCHAR(10) DEFAULT 'FIAT',
    image_url VARCHAR(255),
    enabled BOOLEAN DEFAULT true,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system'
);

CREATE INDEX IF NOT EXISTS idx_currency_code ON currency(code);
CREATE INDEX IF NOT EXISTS idx_currency_enabled ON currency(enabled);
CREATE INDEX IF NOT EXISTS idx_currency_type ON currency(type);

-- Organization Currency table
CREATE TABLE IF NOT EXISTS organization_currency (
    id SERIAL PRIMARY KEY,
    client_id VARCHAR(255),
    currency_code VARCHAR(10),
    is_primary BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    UNIQUE(client_id, currency_code)
);

CREATE INDEX IF NOT EXISTS idx_org_currency_client_id ON organization_currency(client_id);
CREATE INDEX IF NOT EXISTS idx_org_currency_enabled ON organization_currency(client_id, enabled) WHERE enabled = TRUE;

-- =====================================================
-- Indexes
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_organization_slug ON organization(slug);
CREATE INDEX IF NOT EXISTS idx_organization_status ON organization(status);
CREATE INDEX IF NOT EXISTS idx_oauth_user_username ON oauth_user(username);
CREATE INDEX IF NOT EXISTS idx_oauth_user_lockout ON oauth_user (username, locked_until, failed_login_attempts);
CREATE INDEX IF NOT EXISTS idx_oauth_user_registration_source ON oauth_user(registration_source);
CREATE INDEX IF NOT EXISTS idx_oauth_user_organization_id ON oauth_user(organization_id);
CREATE INDEX IF NOT EXISTS idx_oauth_registered_client_client_id ON oauth_registered_client(client_id);
CREATE INDEX IF NOT EXISTS idx_oauth_registered_client_lockout ON oauth_registered_client (client_id, locked_until, failed_auth_attempts);
CREATE UNIQUE INDEX IF NOT EXISTS idx_oauth_registered_client_domain ON oauth_registered_client(domain);
CREATE INDEX IF NOT EXISTS idx_oauth_registered_client_status ON oauth_registered_client(status);
CREATE INDEX IF NOT EXISTS idx_oauth_client_org_id ON oauth_registered_client(organization_id);
CREATE INDEX IF NOT EXISTS idx_oauth_client_kb_id ON oauth_registered_client(knowledge_base_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_oauth_client_kb_id_unique ON oauth_registered_client(knowledge_base_id) WHERE knowledge_base_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_oauth_provider_user_id ON oauth_provider_account(user_id);
CREATE INDEX IF NOT EXISTS idx_oauth_provider_email ON oauth_provider_account(provider_email);
CREATE INDEX IF NOT EXISTS idx_oauth_provider ON oauth_provider_account(provider, provider_user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_jti ON refresh_token(jti);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refresh_token(expires_at);
CREATE INDEX IF NOT EXISTS idx_trusted_device_user_id ON trusted_device(user_id);
CREATE INDEX IF NOT EXISTS idx_trusted_device_fingerprint_hash ON trusted_device(device_fingerprint_hash);
CREATE INDEX IF NOT EXISTS idx_trusted_device_expires_at ON trusted_device(expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_trusted_device_user_fingerprint ON trusted_device(user_id, device_fingerprint_hash);
CREATE INDEX IF NOT EXISTS idx_rate_limit_method_scope_tier ON rate_limit_config(method_name, scope, subscription_tier, active);
CREATE INDEX IF NOT EXISTS idx_country_enabled ON country(enabled);
CREATE INDEX IF NOT EXISTS idx_country_iso2_code ON country(iso2_code);
CREATE INDEX IF NOT EXISTS idx_country_name ON country(name);

-- =====================================================
-- Seed Data: OAuth Scopes, Auth Methods, Grant Types
-- =====================================================
INSERT INTO oauth_scope (name) VALUES
    ('openid'), ('profile'), ('email'),
    ('payments:read'), ('payments:create'),
    ('transfers:read'), ('transfers:create'),
    ('fx:rates'), ('fx:convert'),
    ('bills:read'), ('bills:pay'),
    ('accounts:read'), ('accounts:transactions'),
    ('service:audit'), ('service:notification'), ('service:core'), ('internal'), ('internal:read'),
    ('read'), ('write')
ON CONFLICT (name) DO NOTHING;

INSERT INTO oauth_authentication_method (name) VALUES
    ('client_secret_basic'), ('client_secret_post'), ('none')
ON CONFLICT (name) DO NOTHING;

INSERT INTO oauth_grant_type (name) VALUES
    ('authorization_code'), ('refresh_token'), ('client_credentials')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- Seed Data: Service Clients
-- Client Secret: RespectTheHangover (bcrypt hashed)
-- =====================================================

-- Identity MS Client
INSERT INTO oauth_registered_client (
    id, client_id, client_id_issued_at, sandbox_client_secret, production_client_secret,
    client_name, failed_auth_attempts, environment_mode, status, plan, version, created_at
) VALUES (
    'f47ac10b-58cc-4372-a567-0e02b2c3d479',
    'identity-ms-client',
    NOW(),
    '$2a$12$Ns7eSuHmxpolOqgVXnBPRudDyM/Wiqk5BU8zGHbIbCrkbrMlXHd0i',
    '$2a$12$Ns7eSuHmxpolOqgVXnBPRudDyM/Wiqk5BU8zGHbIbCrkbrMlXHd0i',
    'Identity MS Service Client',
    0, 'SANDBOX', 'ACTIVE', 'TRIAL', 0, NOW()
) ON CONFLICT (client_id) DO NOTHING;

-- Core MS Client
INSERT INTO oauth_registered_client (
    id, client_id, client_id_issued_at, sandbox_client_secret, production_client_secret,
    client_name, failed_auth_attempts, environment_mode, status, plan, version, created_at
) VALUES (
    'a1b2c3d4-5e6f-7890-abcd-ef1234567890',
    'core-ms-client',
    NOW(),
    '$2a$12$Ns7eSuHmxpolOqgVXnBPRudDyM/Wiqk5BU8zGHbIbCrkbrMlXHd0i',
    '$2a$12$Ns7eSuHmxpolOqgVXnBPRudDyM/Wiqk5BU8zGHbIbCrkbrMlXHd0i',
    'Core MS Service Client',
    0, 'SANDBOX', 'ACTIVE', 'TRIAL', 0, NOW()
) ON CONFLICT (client_id) DO NOTHING;

-- Audit MS Client
INSERT INTO oauth_registered_client (
    id, client_id, client_id_issued_at, sandbox_client_secret, production_client_secret,
    client_name, failed_auth_attempts, environment_mode, status, plan, version, created_at
) VALUES (
    'b2c3d4e5-6f78-90ab-cdef-123456789012',
    'audit-ms',
    NOW(),
    '$2a$12$Ns7eSuHmxpolOqgVXnBPRudDyM/Wiqk5BU8zGHbIbCrkbrMlXHd0i',
    '$2a$12$Ns7eSuHmxpolOqgVXnBPRudDyM/Wiqk5BU8zGHbIbCrkbrMlXHd0i',
    'Audit MS Service Client',
    0, 'SANDBOX', 'ACTIVE', 'TRIAL', 0, NOW()
) ON CONFLICT (client_id) DO NOTHING;

-- Notification MS Client
INSERT INTO oauth_registered_client (
    id, client_id, client_id_issued_at, sandbox_client_secret, production_client_secret,
    client_name, failed_auth_attempts, environment_mode, status, plan, version, created_at
) VALUES (
    'c3d4e5f6-7890-abcd-ef12-345678901234',
    'notification-ms',
    NOW(),
    '$2a$12$Ns7eSuHmxpolOqgVXnBPRudDyM/Wiqk5BU8zGHbIbCrkbrMlXHd0i',
    '$2a$12$Ns7eSuHmxpolOqgVXnBPRudDyM/Wiqk5BU8zGHbIbCrkbrMlXHd0i',
    'Notification MS Service Client',
    0, 'SANDBOX', 'ACTIVE', 'TRIAL', 0, NOW()
) ON CONFLICT (client_id) DO NOTHING;

-- =====================================================
-- Seed Data: Client Scopes
-- =====================================================
INSERT INTO oauth_client_scope (registered_client_id, scope_id)
SELECT 'f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, id FROM oauth_scope WHERE name IN ('service:audit', 'service:notification', 'service:core')
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_scope (registered_client_id, scope_id)
SELECT 'a1b2c3d4-5e6f-7890-abcd-ef1234567890'::uuid, id FROM oauth_scope WHERE name IN ('openid', 'profile', 'internal:read')
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_scope (registered_client_id, scope_id)
SELECT 'b2c3d4e5-6f78-90ab-cdef-123456789012'::uuid, id FROM oauth_scope WHERE name IN ('openid', 'profile', 'internal:read')
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_scope (registered_client_id, scope_id)
SELECT 'c3d4e5f6-7890-abcd-ef12-345678901234'::uuid, id FROM oauth_scope WHERE name IN ('openid', 'profile', 'internal:read')
ON CONFLICT DO NOTHING;

-- =====================================================
-- Seed Data: Client Authentication Methods
-- =====================================================
INSERT INTO oauth_client_authentication_method (registered_client_id, method_id)
SELECT 'f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, id FROM oauth_authentication_method WHERE name = 'client_secret_basic'
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_authentication_method (registered_client_id, method_id)
SELECT 'a1b2c3d4-5e6f-7890-abcd-ef1234567890'::uuid, id FROM oauth_authentication_method WHERE name = 'client_secret_basic'
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_authentication_method (registered_client_id, method_id)
SELECT 'b2c3d4e5-6f78-90ab-cdef-123456789012'::uuid, id FROM oauth_authentication_method WHERE name = 'client_secret_basic'
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_authentication_method (registered_client_id, method_id)
SELECT 'c3d4e5f6-7890-abcd-ef12-345678901234'::uuid, id FROM oauth_authentication_method WHERE name = 'client_secret_basic'
ON CONFLICT DO NOTHING;

-- =====================================================
-- Seed Data: Client Grant Types
-- =====================================================
INSERT INTO oauth_client_grant_type (registered_client_id, grant_type_id)
SELECT 'f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, id FROM oauth_grant_type WHERE name = 'client_credentials'
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_grant_type (registered_client_id, grant_type_id)
SELECT 'a1b2c3d4-5e6f-7890-abcd-ef1234567890'::uuid, id FROM oauth_grant_type WHERE name = 'client_credentials'
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_grant_type (registered_client_id, grant_type_id)
SELECT 'b2c3d4e5-6f78-90ab-cdef-123456789012'::uuid, id FROM oauth_grant_type WHERE name = 'client_credentials'
ON CONFLICT DO NOTHING;

INSERT INTO oauth_client_grant_type (registered_client_id, grant_type_id)
SELECT 'c3d4e5f6-7890-abcd-ef12-345678901234'::uuid, id FROM oauth_grant_type WHERE name = 'client_credentials'
ON CONFLICT DO NOTHING;
-- =====================================================
-- Seed Data: Client Token Settings
-- =====================================================
INSERT INTO oauth_client_token_setting (registered_client_id, setting_name, setting_value) VALUES
    ('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'ACCESS_TOKEN_TIME_TO_LIVE', 'PT10M'),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'REUSE_REFRESH_TOKENS', 'false'),
    ('a1b2c3d4-5e6f-7890-abcd-ef1234567890', 'ACCESS_TOKEN_TIME_TO_LIVE', 'PT10M'),
    ('a1b2c3d4-5e6f-7890-abcd-ef1234567890', 'REUSE_REFRESH_TOKENS', 'false'),
    ('b2c3d4e5-6f78-90ab-cdef-123456789012', 'ACCESS_TOKEN_TIME_TO_LIVE', 'PT10M'),
    ('b2c3d4e5-6f78-90ab-cdef-123456789012', 'REUSE_REFRESH_TOKENS', 'false'),
    ('c3d4e5f6-7890-abcd-ef12-345678901234', 'ACCESS_TOKEN_TIME_TO_LIVE', 'PT10M'),
    ('c3d4e5f6-7890-abcd-ef12-345678901234', 'REUSE_REFRESH_TOKENS', 'false')
ON CONFLICT DO NOTHING;

-- =====================================================
-- Seed Data: Rate Limit Config
-- =====================================================
INSERT INTO rate_limit_config (method_name, subscription_tier, scope, capacity, time_value, time_unit) VALUES
('login-attempts', 'TRIAL', 'INDIVIDUAL', 5, 1, 'MINUTES'),
('login-attempts', 'STARTER', 'INDIVIDUAL', 10, 1, 'MINUTES'),
('login-attempts', 'PROFESSIONAL', 'INDIVIDUAL', 15, 1, 'MINUTES'),
('login-attempts', 'ENTERPRISE', 'INDIVIDUAL', 20, 1, 'MINUTES'),
('2fa-resend', 'TRIAL', 'INDIVIDUAL', 1, 1, 'MINUTES'),
('2fa-resend', 'STARTER', 'INDIVIDUAL', 2, 1, 'MINUTES'),
('2fa-resend', 'PROFESSIONAL', 'INDIVIDUAL', 3, 1, 'MINUTES'),
('2fa-resend', 'ENTERPRISE', 'INDIVIDUAL', 5, 1, 'MINUTES'),
('2fa-verify', 'TRIAL', 'INDIVIDUAL', 5, 1, 'MINUTES'),
('2fa-verify', 'STARTER', 'INDIVIDUAL', 10, 1, 'MINUTES'),
('2fa-verify', 'PROFESSIONAL', 'INDIVIDUAL', 15, 1, 'MINUTES'),
('2fa-verify', 'ENTERPRISE', 'INDIVIDUAL', 20, 1, 'MINUTES'),
('password-reset', 'TRIAL', 'INDIVIDUAL', 2, 5, 'MINUTES'),
('password-reset', 'STARTER', 'INDIVIDUAL', 3, 5, 'MINUTES'),
('password-reset', 'PROFESSIONAL', 'INDIVIDUAL', 5, 5, 'MINUTES'),
('password-reset', 'ENTERPRISE', 'INDIVIDUAL', 10, 5, 'MINUTES'),
('api-request', 'TRIAL', 'ORGANIZATION', 1000, 1, 'HOURS'),
('api-request', 'STARTER', 'ORGANIZATION', 5000, 1, 'HOURS'),
('api-request', 'PROFESSIONAL', 'ORGANIZATION', 25000, 1, 'HOURS'),
('api-request', 'ENTERPRISE', 'ORGANIZATION', 100000, 1, 'HOURS')
ON CONFLICT (method_name, scope, subscription_tier) DO NOTHING;

-- =====================================================
-- Seed Data: Countries
-- =====================================================
INSERT INTO country (name, iso2_code, iso3_code, numeric_code, dial_code, flag_url, region, sub_region, enabled) VALUES
('Afghanistan', 'AF', 'AFG', '004', '+93', 'https://flagcdn.com/af.svg', 'Asia', 'Southern Asia', false),
('Albania', 'AL', 'ALB', '008', '+355', 'https://flagcdn.com/al.svg', 'Europe', 'Southern Europe', false),
('Algeria', 'DZ', 'DZA', '012', '+213', 'https://flagcdn.com/dz.svg', 'Africa', 'Northern Africa', false),
('Andorra', 'AD', 'AND', '020', '+376', 'https://flagcdn.com/ad.svg', 'Europe', 'Southern Europe', false),
('Angola', 'AO', 'AGO', '024', '+244', 'https://flagcdn.com/ao.svg', 'Africa', 'Sub-Saharan Africa', false),
('Antigua and Barbuda', 'AG', 'ATG', '028', '+1268', 'https://flagcdn.com/ag.svg', 'Americas', 'Caribbean', false),
('Argentina', 'AR', 'ARG', '032', '+54', 'https://flagcdn.com/ar.svg', 'Americas', 'South America', false),
('Armenia', 'AM', 'ARM', '051', '+374', 'https://flagcdn.com/am.svg', 'Asia', 'Western Asia', false),
('Australia', 'AU', 'AUS', '036', '+61', 'https://flagcdn.com/au.svg', 'Oceania', 'Australia and New Zealand', true),
('Austria', 'AT', 'AUT', '040', '+43', 'https://flagcdn.com/at.svg', 'Europe', 'Western Europe', false),
('Azerbaijan', 'AZ', 'AZE', '031', '+994', 'https://flagcdn.com/az.svg', 'Asia', 'Western Asia', false),
('Bahamas', 'BS', 'BHS', '044', '+1242', 'https://flagcdn.com/bs.svg', 'Americas', 'Caribbean', false),
('Bahrain', 'BH', 'BHR', '048', '+973', 'https://flagcdn.com/bh.svg', 'Asia', 'Western Asia', false),
('Bangladesh', 'BD', 'BGD', '050', '+880', 'https://flagcdn.com/bd.svg', 'Asia', 'Southern Asia', false),
('Barbados', 'BB', 'BRB', '052', '+1246', 'https://flagcdn.com/bb.svg', 'Americas', 'Caribbean', false),
('Belarus', 'BY', 'BLR', '112', '+375', 'https://flagcdn.com/by.svg', 'Europe', 'Eastern Europe', false),
('Belgium', 'BE', 'BEL', '056', '+32', 'https://flagcdn.com/be.svg', 'Europe', 'Western Europe', false),
('Belize', 'BZ', 'BLZ', '084', '+501', 'https://flagcdn.com/bz.svg', 'Americas', 'Central America', false),
('Benin', 'BJ', 'BEN', '204', '+229', 'https://flagcdn.com/bj.svg', 'Africa', 'Western Africa', false),
('Bhutan', 'BT', 'BTN', '064', '+975', 'https://flagcdn.com/bt.svg', 'Asia', 'Southern Asia', false),
('Bolivia', 'BO', 'BOL', '068', '+591', 'https://flagcdn.com/bo.svg', 'Americas', 'South America', false),
('Bosnia and Herzegovina', 'BA', 'BIH', '070', '+387', 'https://flagcdn.com/ba.svg', 'Europe', 'Southern Europe', false),
('Botswana', 'BW', 'BWA', '072', '+267', 'https://flagcdn.com/bw.svg', 'Africa', 'Southern Africa', false),
('Brazil', 'BR', 'BRA', '076', '+55', 'https://flagcdn.com/br.svg', 'Americas', 'South America', true),
('Brunei', 'BN', 'BRN', '096', '+673', 'https://flagcdn.com/bn.svg', 'Asia', 'South-eastern Asia', false),
('Bulgaria', 'BG', 'BGR', '100', '+359', 'https://flagcdn.com/bg.svg', 'Europe', 'Eastern Europe', false),
('Burkina Faso', 'BF', 'BFA', '854', '+226', 'https://flagcdn.com/bf.svg', 'Africa', 'Western Africa', false),
('Burundi', 'BI', 'BDI', '108', '+257', 'https://flagcdn.com/bi.svg', 'Africa', 'Eastern Africa', false),
('Cambodia', 'KH', 'KHM', '116', '+855', 'https://flagcdn.com/kh.svg', 'Asia', 'South-eastern Asia', false),
('Cameroon', 'CM', 'CMR', '120', '+237', 'https://flagcdn.com/cm.svg', 'Africa', 'Middle Africa', false),
('Canada', 'CA', 'CAN', '124', '+1', 'https://flagcdn.com/ca.svg', 'Americas', 'Northern America', true),
('Cape Verde', 'CV', 'CPV', '132', '+238', 'https://flagcdn.com/cv.svg', 'Africa', 'Western Africa', false),
('Central African Republic', 'CF', 'CAF', '140', '+236', 'https://flagcdn.com/cf.svg', 'Africa', 'Middle Africa', false),
('Chad', 'TD', 'TCD', '148', '+235', 'https://flagcdn.com/td.svg', 'Africa', 'Middle Africa', false),
('Chile', 'CL', 'CHL', '152', '+56', 'https://flagcdn.com/cl.svg', 'Americas', 'South America', false),
('China', 'CN', 'CHN', '156', '+86', 'https://flagcdn.com/cn.svg', 'Asia', 'Eastern Asia', false),
('Colombia', 'CO', 'COL', '170', '+57', 'https://flagcdn.com/co.svg', 'Americas', 'South America', false),
('Comoros', 'KM', 'COM', '174', '+269', 'https://flagcdn.com/km.svg', 'Africa', 'Eastern Africa', false),
('Congo', 'CG', 'COG', '178', '+242', 'https://flagcdn.com/cg.svg', 'Africa', 'Middle Africa', false),
('Costa Rica', 'CR', 'CRI', '188', '+506', 'https://flagcdn.com/cr.svg', 'Americas', 'Central America', false),
('Croatia', 'HR', 'HRV', '191', '+385', 'https://flagcdn.com/hr.svg', 'Europe', 'Southern Europe', false),
('Cuba', 'CU', 'CUB', '192', '+53', 'https://flagcdn.com/cu.svg', 'Americas', 'Caribbean', false),
('Cyprus', 'CY', 'CYP', '196', '+357', 'https://flagcdn.com/cy.svg', 'Asia', 'Western Asia', false),
('Czech Republic', 'CZ', 'CZE', '203', '+420', 'https://flagcdn.com/cz.svg', 'Europe', 'Eastern Europe', false),
('Denmark', 'DK', 'DNK', '208', '+45', 'https://flagcdn.com/dk.svg', 'Europe', 'Northern Europe', false),
('Djibouti', 'DJ', 'DJI', '262', '+253', 'https://flagcdn.com/dj.svg', 'Africa', 'Eastern Africa', false),
('Dominica', 'DM', 'DMA', '212', '+1767', 'https://flagcdn.com/dm.svg', 'Americas', 'Caribbean', false),
('Dominican Republic', 'DO', 'DOM', '214', '+1809', 'https://flagcdn.com/do.svg', 'Americas', 'Caribbean', false),
('Ecuador', 'EC', 'ECU', '218', '+593', 'https://flagcdn.com/ec.svg', 'Americas', 'South America', false),
('Egypt', 'EG', 'EGY', '818', '+20', 'https://flagcdn.com/eg.svg', 'Africa', 'Northern Africa', false),
('El Salvador', 'SV', 'SLV', '222', '+503', 'https://flagcdn.com/sv.svg', 'Americas', 'Central America', false),
('Equatorial Guinea', 'GQ', 'GNQ', '226', '+240', 'https://flagcdn.com/gq.svg', 'Africa', 'Middle Africa', false),
('Eritrea', 'ER', 'ERI', '232', '+291', 'https://flagcdn.com/er.svg', 'Africa', 'Eastern Africa', false),
('Estonia', 'EE', 'EST', '233', '+372', 'https://flagcdn.com/ee.svg', 'Europe', 'Northern Europe', false),
('Eswatini', 'SZ', 'SWZ', '748', '+268', 'https://flagcdn.com/sz.svg', 'Africa', 'Southern Africa', false),
('Ethiopia', 'ET', 'ETH', '231', '+251', 'https://flagcdn.com/et.svg', 'Africa', 'Eastern Africa', false),
('Fiji', 'FJ', 'FJI', '242', '+679', 'https://flagcdn.com/fj.svg', 'Oceania', 'Melanesia', false),
('Finland', 'FI', 'FIN', '246', '+358', 'https://flagcdn.com/fi.svg', 'Europe', 'Northern Europe', false),
('France', 'FR', 'FRA', '250', '+33', 'https://flagcdn.com/fr.svg', 'Europe', 'Western Europe', true),
('Gabon', 'GA', 'GAB', '266', '+241', 'https://flagcdn.com/ga.svg', 'Africa', 'Middle Africa', false),
('Gambia', 'GM', 'GMB', '270', '+220', 'https://flagcdn.com/gm.svg', 'Africa', 'Western Africa', false),
('Georgia', 'GE', 'GEO', '268', '+995', 'https://flagcdn.com/ge.svg', 'Asia', 'Western Asia', false),
('Germany', 'DE', 'DEU', '276', '+49', 'https://flagcdn.com/de.svg', 'Europe', 'Western Europe', true),
('Ghana', 'GH', 'GHA', '288', '+233', 'https://flagcdn.com/gh.svg', 'Africa', 'Western Africa', false),
('Greece', 'GR', 'GRC', '300', '+30', 'https://flagcdn.com/gr.svg', 'Europe', 'Southern Europe', false),
('Grenada', 'GD', 'GRD', '308', '+1473', 'https://flagcdn.com/gd.svg', 'Americas', 'Caribbean', false),
('Guatemala', 'GT', 'GTM', '320', '+502', 'https://flagcdn.com/gt.svg', 'Americas', 'Central America', false),
('Guinea', 'GN', 'GIN', '324', '+224', 'https://flagcdn.com/gn.svg', 'Africa', 'Western Africa', false),
('Guinea-Bissau', 'GW', 'GNB', '624', '+245', 'https://flagcdn.com/gw.svg', 'Africa', 'Western Africa', false),
('Guyana', 'GY', 'GUY', '328', '+592', 'https://flagcdn.com/gy.svg', 'Americas', 'South America', false),
('Haiti', 'HT', 'HTI', '332', '+509', 'https://flagcdn.com/ht.svg', 'Americas', 'Caribbean', false),
('Honduras', 'HN', 'HND', '340', '+504', 'https://flagcdn.com/hn.svg', 'Americas', 'Central America', false),
('Hong Kong', 'HK', 'HKG', '344', '+852', 'https://flagcdn.com/hk.svg', 'Asia', 'Eastern Asia', false),
('Hungary', 'HU', 'HUN', '348', '+36', 'https://flagcdn.com/hu.svg', 'Europe', 'Eastern Europe', false),
('Iceland', 'IS', 'ISL', '352', '+354', 'https://flagcdn.com/is.svg', 'Europe', 'Northern Europe', false),
('India', 'IN', 'IND', '356', '+91', 'https://flagcdn.com/in.svg', 'Asia', 'Southern Asia', true),
('Indonesia', 'ID', 'IDN', '360', '+62', 'https://flagcdn.com/id.svg', 'Asia', 'South-eastern Asia', false),
('Iran', 'IR', 'IRN', '364', '+98', 'https://flagcdn.com/ir.svg', 'Asia', 'Southern Asia', false),
('Iraq', 'IQ', 'IRQ', '368', '+964', 'https://flagcdn.com/iq.svg', 'Asia', 'Western Asia', false),
('Ireland', 'IE', 'IRL', '372', '+353', 'https://flagcdn.com/ie.svg', 'Europe', 'Northern Europe', false),
('Israel', 'IL', 'ISR', '376', '+972', 'https://flagcdn.com/il.svg', 'Asia', 'Western Asia', false),
('Italy', 'IT', 'ITA', '380', '+39', 'https://flagcdn.com/it.svg', 'Europe', 'Southern Europe', false),
('Ivory Coast', 'CI', 'CIV', '384', '+225', 'https://flagcdn.com/ci.svg', 'Africa', 'Western Africa', false),
('Jamaica', 'JM', 'JAM', '388', '+1876', 'https://flagcdn.com/jm.svg', 'Americas', 'Caribbean', false),
('Japan', 'JP', 'JPN', '392', '+81', 'https://flagcdn.com/jp.svg', 'Asia', 'Eastern Asia', true),
('Jordan', 'JO', 'JOR', '400', '+962', 'https://flagcdn.com/jo.svg', 'Asia', 'Western Asia', false),
('Kazakhstan', 'KZ', 'KAZ', '398', '+7', 'https://flagcdn.com/kz.svg', 'Asia', 'Central Asia', false),
('Kenya', 'KE', 'KEN', '404', '+254', 'https://flagcdn.com/ke.svg', 'Africa', 'Eastern Africa', false),
('Kiribati', 'KI', 'KIR', '296', '+686', 'https://flagcdn.com/ki.svg', 'Oceania', 'Micronesia', false),
('Kuwait', 'KW', 'KWT', '414', '+965', 'https://flagcdn.com/kw.svg', 'Asia', 'Western Asia', false),
('Kyrgyzstan', 'KG', 'KGZ', '417', '+996', 'https://flagcdn.com/kg.svg', 'Asia', 'Central Asia', false),
('Laos', 'LA', 'LAO', '418', '+856', 'https://flagcdn.com/la.svg', 'Asia', 'South-eastern Asia', false),
('Latvia', 'LV', 'LVA', '428', '+371', 'https://flagcdn.com/lv.svg', 'Europe', 'Northern Europe', false),
('Lebanon', 'LB', 'LBN', '422', '+961', 'https://flagcdn.com/lb.svg', 'Asia', 'Western Asia', false),
('Lesotho', 'LS', 'LSO', '426', '+266', 'https://flagcdn.com/ls.svg', 'Africa', 'Southern Africa', false),
('Liberia', 'LR', 'LBR', '430', '+231', 'https://flagcdn.com/lr.svg', 'Africa', 'Western Africa', false),
('Libya', 'LY', 'LBY', '434', '+218', 'https://flagcdn.com/ly.svg', 'Africa', 'Northern Africa', false),
('Liechtenstein', 'LI', 'LIE', '438', '+423', 'https://flagcdn.com/li.svg', 'Europe', 'Western Europe', false),
('Lithuania', 'LT', 'LTU', '440', '+370', 'https://flagcdn.com/lt.svg', 'Europe', 'Northern Europe', false),
('Luxembourg', 'LU', 'LUX', '442', '+352', 'https://flagcdn.com/lu.svg', 'Europe', 'Western Europe', false),
('Macau', 'MO', 'MAC', '446', '+853', 'https://flagcdn.com/mo.svg', 'Asia', 'Eastern Asia', false),
('Madagascar', 'MG', 'MDG', '450', '+261', 'https://flagcdn.com/mg.svg', 'Africa', 'Eastern Africa', false),
('Malawi', 'MW', 'MWI', '454', '+265', 'https://flagcdn.com/mw.svg', 'Africa', 'Eastern Africa', false),
('Malaysia', 'MY', 'MYS', '458', '+60', 'https://flagcdn.com/my.svg', 'Asia', 'South-eastern Asia', false),
('Maldives', 'MV', 'MDV', '462', '+960', 'https://flagcdn.com/mv.svg', 'Asia', 'Southern Asia', false),
('Mali', 'ML', 'MLI', '466', '+223', 'https://flagcdn.com/ml.svg', 'Africa', 'Western Africa', false),
('Malta', 'MT', 'MLT', '470', '+356', 'https://flagcdn.com/mt.svg', 'Europe', 'Southern Europe', false),
('Marshall Islands', 'MH', 'MHL', '584', '+692', 'https://flagcdn.com/mh.svg', 'Oceania', 'Micronesia', false),
('Mauritania', 'MR', 'MRT', '478', '+222', 'https://flagcdn.com/mr.svg', 'Africa', 'Western Africa', false),
('Mauritius', 'MU', 'MUS', '480', '+230', 'https://flagcdn.com/mu.svg', 'Africa', 'Eastern Africa', false),
('Mexico', 'MX', 'MEX', '484', '+52', 'https://flagcdn.com/mx.svg', 'Americas', 'Central America', false),
('Micronesia', 'FM', 'FSM', '583', '+691', 'https://flagcdn.com/fm.svg', 'Oceania', 'Micronesia', false),
('Moldova', 'MD', 'MDA', '498', '+373', 'https://flagcdn.com/md.svg', 'Europe', 'Eastern Europe', false),
('Monaco', 'MC', 'MCO', '492', '+377', 'https://flagcdn.com/mc.svg', 'Europe', 'Western Europe', false),
('Mongolia', 'MN', 'MNG', '496', '+976', 'https://flagcdn.com/mn.svg', 'Asia', 'Eastern Asia', false),
('Montenegro', 'ME', 'MNE', '499', '+382', 'https://flagcdn.com/me.svg', 'Europe', 'Southern Europe', false),
('Morocco', 'MA', 'MAR', '504', '+212', 'https://flagcdn.com/ma.svg', 'Africa', 'Northern Africa', false),
('Mozambique', 'MZ', 'MOZ', '508', '+258', 'https://flagcdn.com/mz.svg', 'Africa', 'Eastern Africa', false),
('Myanmar', 'MM', 'MMR', '104', '+95', 'https://flagcdn.com/mm.svg', 'Asia', 'South-eastern Asia', false),
('Namibia', 'NA', 'NAM', '516', '+264', 'https://flagcdn.com/na.svg', 'Africa', 'Southern Africa', false),
('Nauru', 'NR', 'NRU', '520', '+674', 'https://flagcdn.com/nr.svg', 'Oceania', 'Micronesia', false),
('Nepal', 'NP', 'NPL', '524', '+977', 'https://flagcdn.com/np.svg', 'Asia', 'Southern Asia', false),
('Netherlands', 'NL', 'NLD', '528', '+31', 'https://flagcdn.com/nl.svg', 'Europe', 'Western Europe', true),
('New Zealand', 'NZ', 'NZL', '554', '+64', 'https://flagcdn.com/nz.svg', 'Oceania', 'Australia and New Zealand', false),
('Nicaragua', 'NI', 'NIC', '558', '+505', 'https://flagcdn.com/ni.svg', 'Americas', 'Central America', false),
('Niger', 'NE', 'NER', '562', '+227', 'https://flagcdn.com/ne.svg', 'Africa', 'Western Africa', false),
('Nigeria', 'NG', 'NGA', '566', '+234', 'https://flagcdn.com/ng.svg', 'Africa', 'Western Africa', false),
('North Korea', 'KP', 'PRK', '408', '+850', 'https://flagcdn.com/kp.svg', 'Asia', 'Eastern Asia', false),
('North Macedonia', 'MK', 'MKD', '807', '+389', 'https://flagcdn.com/mk.svg', 'Europe', 'Southern Europe', false),
('Norway', 'NO', 'NOR', '578', '+47', 'https://flagcdn.com/no.svg', 'Europe', 'Northern Europe', false),
('Oman', 'OM', 'OMN', '512', '+968', 'https://flagcdn.com/om.svg', 'Asia', 'Western Asia', false),
('Pakistan', 'PK', 'PAK', '586', '+92', 'https://flagcdn.com/pk.svg', 'Asia', 'Southern Asia', false),
('Palau', 'PW', 'PLW', '585', '+680', 'https://flagcdn.com/pw.svg', 'Oceania', 'Micronesia', false),
('Palestine', 'PS', 'PSE', '275', '+970', 'https://flagcdn.com/ps.svg', 'Asia', 'Western Asia', false),
('Panama', 'PA', 'PAN', '591', '+507', 'https://flagcdn.com/pa.svg', 'Americas', 'Central America', false),
('Papua New Guinea', 'PG', 'PNG', '598', '+675', 'https://flagcdn.com/pg.svg', 'Oceania', 'Melanesia', false),
('Paraguay', 'PY', 'PRY', '600', '+595', 'https://flagcdn.com/py.svg', 'Americas', 'South America', false),
('Peru', 'PE', 'PER', '604', '+51', 'https://flagcdn.com/pe.svg', 'Americas', 'South America', false),
('Philippines', 'PH', 'PHL', '608', '+63', 'https://flagcdn.com/ph.svg', 'Asia', 'South-eastern Asia', false),
('Poland', 'PL', 'POL', '616', '+48', 'https://flagcdn.com/pl.svg', 'Europe', 'Eastern Europe', false),
('Portugal', 'PT', 'PRT', '620', '+351', 'https://flagcdn.com/pt.svg', 'Europe', 'Southern Europe', false),
('Qatar', 'QA', 'QAT', '634', '+974', 'https://flagcdn.com/qa.svg', 'Asia', 'Western Asia', false),
('Romania', 'RO', 'ROU', '642', '+40', 'https://flagcdn.com/ro.svg', 'Europe', 'Eastern Europe', false),
('Russia', 'RU', 'RUS', '643', '+7', 'https://flagcdn.com/ru.svg', 'Europe', 'Eastern Europe', false),
('Rwanda', 'RW', 'RWA', '646', '+250', 'https://flagcdn.com/rw.svg', 'Africa', 'Eastern Africa', false),
('Saint Kitts and Nevis', 'KN', 'KNA', '659', '+1869', 'https://flagcdn.com/kn.svg', 'Americas', 'Caribbean', false),
('Saint Lucia', 'LC', 'LCA', '662', '+1758', 'https://flagcdn.com/lc.svg', 'Americas', 'Caribbean', false),
('Saint Vincent and the Grenadines', 'VC', 'VCT', '670', '+1784', 'https://flagcdn.com/vc.svg', 'Americas', 'Caribbean', false),
('Samoa', 'WS', 'WSM', '882', '+685', 'https://flagcdn.com/ws.svg', 'Oceania', 'Polynesia', false),
('San Marino', 'SM', 'SMR', '674', '+378', 'https://flagcdn.com/sm.svg', 'Europe', 'Southern Europe', false),
('Sao Tome and Principe', 'ST', 'STP', '678', '+239', 'https://flagcdn.com/st.svg', 'Africa', 'Middle Africa', false),
('Saudi Arabia', 'SA', 'SAU', '682', '+966', 'https://flagcdn.com/sa.svg', 'Asia', 'Western Asia', false),
('Senegal', 'SN', 'SEN', '686', '+221', 'https://flagcdn.com/sn.svg', 'Africa', 'Western Africa', false),
('Serbia', 'RS', 'SRB', '688', '+381', 'https://flagcdn.com/rs.svg', 'Europe', 'Southern Europe', false),
('Seychelles', 'SC', 'SYC', '690', '+248', 'https://flagcdn.com/sc.svg', 'Africa', 'Eastern Africa', false),
('Sierra Leone', 'SL', 'SLE', '694', '+232', 'https://flagcdn.com/sl.svg', 'Africa', 'Western Africa', false),
('Singapore', 'SG', 'SGP', '702', '+65', 'https://flagcdn.com/sg.svg', 'Asia', 'South-eastern Asia', true),
('Slovakia', 'SK', 'SVK', '703', '+421', 'https://flagcdn.com/sk.svg', 'Europe', 'Eastern Europe', false),
('Slovenia', 'SI', 'SVN', '705', '+386', 'https://flagcdn.com/si.svg', 'Europe', 'Southern Europe', false),
('Solomon Islands', 'SB', 'SLB', '090', '+677', 'https://flagcdn.com/sb.svg', 'Oceania', 'Melanesia', false),
('Somalia', 'SO', 'SOM', '706', '+252', 'https://flagcdn.com/so.svg', 'Africa', 'Eastern Africa', false),
('South Africa', 'ZA', 'ZAF', '710', '+27', 'https://flagcdn.com/za.svg', 'Africa', 'Southern Africa', false),
('South Korea', 'KR', 'KOR', '410', '+82', 'https://flagcdn.com/kr.svg', 'Asia', 'Eastern Asia', false),
('South Sudan', 'SS', 'SSD', '728', '+211', 'https://flagcdn.com/ss.svg', 'Africa', 'Eastern Africa', false),
('Spain', 'ES', 'ESP', '724', '+34', 'https://flagcdn.com/es.svg', 'Europe', 'Southern Europe', false),
('Sri Lanka', 'LK', 'LKA', '144', '+94', 'https://flagcdn.com/lk.svg', 'Asia', 'Southern Asia', false),
('Sudan', 'SD', 'SDN', '729', '+249', 'https://flagcdn.com/sd.svg', 'Africa', 'Northern Africa', false),
('Suriname', 'SR', 'SUR', '740', '+597', 'https://flagcdn.com/sr.svg', 'Americas', 'South America', false),
('Sweden', 'SE', 'SWE', '752', '+46', 'https://flagcdn.com/se.svg', 'Europe', 'Northern Europe', false),
('Switzerland', 'CH', 'CHE', '756', '+41', 'https://flagcdn.com/ch.svg', 'Europe', 'Western Europe', false),
('Syria', 'SY', 'SYR', '760', '+963', 'https://flagcdn.com/sy.svg', 'Asia', 'Western Asia', false),
('Taiwan', 'TW', 'TWN', '158', '+886', 'https://flagcdn.com/tw.svg', 'Asia', 'Eastern Asia', false),
('Tajikistan', 'TJ', 'TJK', '762', '+992', 'https://flagcdn.com/tj.svg', 'Asia', 'Central Asia', false),
('Tanzania', 'TZ', 'TZA', '834', '+255', 'https://flagcdn.com/tz.svg', 'Africa', 'Eastern Africa', false),
('Thailand', 'TH', 'THA', '764', '+66', 'https://flagcdn.com/th.svg', 'Asia', 'South-eastern Asia', false),
('Timor-Leste', 'TL', 'TLS', '626', '+670', 'https://flagcdn.com/tl.svg', 'Asia', 'South-eastern Asia', false),
('Togo', 'TG', 'TGO', '768', '+228', 'https://flagcdn.com/tg.svg', 'Africa', 'Western Africa', false),
('Tonga', 'TO', 'TON', '776', '+676', 'https://flagcdn.com/to.svg', 'Oceania', 'Polynesia', false),
('Trinidad and Tobago', 'TT', 'TTO', '780', '+1868', 'https://flagcdn.com/tt.svg', 'Americas', 'Caribbean', false),
('Tunisia', 'TN', 'TUN', '788', '+216', 'https://flagcdn.com/tn.svg', 'Africa', 'Northern Africa', false),
('Turkey', 'TR', 'TUR', '792', '+90', 'https://flagcdn.com/tr.svg', 'Asia', 'Western Asia', false),
('Turkmenistan', 'TM', 'TKM', '795', '+993', 'https://flagcdn.com/tm.svg', 'Asia', 'Central Asia', false),
('Tuvalu', 'TV', 'TUV', '798', '+688', 'https://flagcdn.com/tv.svg', 'Oceania', 'Polynesia', false),
('Uganda', 'UG', 'UGA', '800', '+256', 'https://flagcdn.com/ug.svg', 'Africa', 'Eastern Africa', false),
('Ukraine', 'UA', 'UKR', '804', '+380', 'https://flagcdn.com/ua.svg', 'Europe', 'Eastern Europe', false),
('United Arab Emirates', 'AE', 'ARE', '784', '+971', 'https://flagcdn.com/ae.svg', 'Asia', 'Western Asia', false),
('United Kingdom', 'GB', 'GBR', '826', '+44', 'https://flagcdn.com/gb.svg', 'Europe', 'Northern Europe', true),
('United States', 'US', 'USA', '840', '+1', 'https://flagcdn.com/us.svg', 'Americas', 'Northern America', true),
('Uruguay', 'UY', 'URY', '858', '+598', 'https://flagcdn.com/uy.svg', 'Americas', 'South America', false),
('Uzbekistan', 'UZ', 'UZB', '860', '+998', 'https://flagcdn.com/uz.svg', 'Asia', 'Central Asia', false),
('Vanuatu', 'VU', 'VUT', '548', '+678', 'https://flagcdn.com/vu.svg', 'Oceania', 'Melanesia', false),
('Vatican City', 'VA', 'VAT', '336', '+379', 'https://flagcdn.com/va.svg', 'Europe', 'Southern Europe', false),
('Venezuela', 'VE', 'VEN', '862', '+58', 'https://flagcdn.com/ve.svg', 'Americas', 'South America', false),
('Vietnam', 'VN', 'VNM', '704', '+84', 'https://flagcdn.com/vn.svg', 'Asia', 'South-eastern Asia', false),
('Yemen', 'YE', 'YEM', '887', '+967', 'https://flagcdn.com/ye.svg', 'Asia', 'Western Asia', false),
('Zambia', 'ZM', 'ZMB', '894', '+260', 'https://flagcdn.com/zm.svg', 'Africa', 'Eastern Africa', false),
('Zimbabwe', 'ZW', 'ZWE', '716', '+263', 'https://flagcdn.com/zw.svg', 'Africa', 'Eastern Africa', false)
ON CONFLICT (iso2_code) DO NOTHING;
