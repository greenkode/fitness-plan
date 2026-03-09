ALTER TABLE oauth_user ADD COLUMN IF NOT EXISTS two_factor_last_verified TIMESTAMP;
