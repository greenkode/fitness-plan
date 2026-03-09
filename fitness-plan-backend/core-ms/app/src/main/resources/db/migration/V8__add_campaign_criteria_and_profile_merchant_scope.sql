ALTER TABLE account_profile ADD COLUMN IF NOT EXISTS merchant_id uuid;

ALTER TABLE account_profile DROP CONSTRAINT IF EXISTS ux_account_profile_name;

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_profile_system_name
    ON account_profile (name) WHERE merchant_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_profile_merchant_name
    ON account_profile (merchant_id, name) WHERE merchant_id IS NOT NULL;

ALTER TABLE campaign ADD COLUMN IF NOT EXISTS account_profile_id uuid;
ALTER TABLE campaign ADD COLUMN IF NOT EXISTS transaction_group varchar(50);
ALTER TABLE campaign ADD COLUMN IF NOT EXISTS transaction_template_id uuid;
