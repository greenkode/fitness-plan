ALTER TABLE oauth_user ADD COLUMN IF NOT EXISTS totp_secret TEXT;
ALTER TABLE oauth_user ADD COLUMN IF NOT EXISTS totp_enabled BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS totp_recovery_code (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    used_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT fk_totp_recovery_user FOREIGN KEY (user_id) REFERENCES oauth_user(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_totp_recovery_user_id ON totp_recovery_code(user_id);
CREATE INDEX IF NOT EXISTS idx_totp_recovery_unused ON totp_recovery_code(user_id, used) WHERE used = false;
