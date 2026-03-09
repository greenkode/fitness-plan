ALTER TABLE billing_invoice ADD COLUMN IF NOT EXISTS billing_plan_public_id UUID;
ALTER TABLE billing_invoice ADD COLUMN IF NOT EXISTS billing_plan_name VARCHAR(255);

ALTER TABLE billing_invoice DROP CONSTRAINT IF EXISTS fk_billing_invoice_billing_plan;

ALTER TABLE billing_invoice DROP COLUMN IF EXISTS billing_plan_id;

DROP TABLE IF EXISTS billing_plan;
