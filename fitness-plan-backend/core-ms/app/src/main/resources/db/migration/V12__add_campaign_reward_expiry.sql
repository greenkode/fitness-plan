ALTER TABLE campaign ADD COLUMN IF NOT EXISTS reward_expiry_days INT;

ALTER TABLE campaign_redemption ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE campaign_redemption ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;
ALTER TABLE campaign_redemption ADD COLUMN IF NOT EXISTS reward_transaction_reference UUID;

CREATE INDEX IF NOT EXISTS idx_campaign_redemption_status_expires
    ON campaign_redemption (status, expires_at)
    WHERE status = 'ACTIVE' AND expires_at IS NOT NULL;
