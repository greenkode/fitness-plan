CREATE TABLE IF NOT EXISTS billing_plan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organization(id),
    name VARCHAR(255) NOT NULL,
    platform_fee_cents BIGINT NOT NULL DEFAULT 0,
    per_account_fee_cents BIGINT NOT NULL DEFAULT 0,
    per_transaction_fee_cents BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    billing_cycle VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_billing_plan_org_status ON billing_plan(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_billing_plan_public_id ON billing_plan(public_id);
