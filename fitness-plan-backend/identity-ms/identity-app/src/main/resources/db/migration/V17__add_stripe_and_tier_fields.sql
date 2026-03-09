ALTER TABLE organization ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(255);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS restricted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS restricted_at TIMESTAMPTZ;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS restriction_reason VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_org_stripe_customer ON organization(stripe_customer_id) WHERE stripe_customer_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_org_stripe_subscription ON organization(stripe_subscription_id) WHERE stripe_subscription_id IS NOT NULL;

ALTER TABLE billing_plan ADD COLUMN IF NOT EXISTS subscription_tier VARCHAR(50);
ALTER TABLE billing_plan ADD COLUMN IF NOT EXISTS stripe_price_id VARCHAR(255);
