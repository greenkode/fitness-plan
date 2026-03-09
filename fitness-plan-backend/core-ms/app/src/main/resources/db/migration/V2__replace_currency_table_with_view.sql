CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE IF NOT EXISTS identity.currency (
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

DROP TABLE IF EXISTS currency CASCADE;

CREATE OR REPLACE VIEW currency AS
SELECT
    id,
    name,
    code,
    name AS major_single,
    name AS major_plural,
    COALESCE(iso_num, 0) AS iso_num,
    symbol,
    symbol_native,
    '' AS minor_single,
    '' AS minor_plural,
    COALESCE(image_url, '') AS image_url,
    iso_digits,
    decimals,
    CASE WHEN decimals > 0 THEN POWER(10, decimals)::int ELSE 1 END AS num_to_basic,
    enabled,
    type,
    created_at,
    last_modified_at,
    COALESCE(created_by, 'system') AS created_by,
    COALESCE(last_modified_by, 'system') AS last_modified_by,
    COALESCE(version, 0) AS version
FROM identity.currency;
