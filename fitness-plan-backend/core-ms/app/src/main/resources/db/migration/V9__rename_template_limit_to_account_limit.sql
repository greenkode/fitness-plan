ALTER TABLE IF EXISTS template_limit RENAME TO account_limit;

ALTER TABLE account_limit ADD COLUMN IF NOT EXISTS template_public_id uuid;

UPDATE account_limit al
SET template_public_id = tt.public_id
FROM transaction_template tt
WHERE al.template_name = tt.name
  AND al.merchant_id IS NULL
  AND tt.merchant_id IS NULL
  AND al.template_public_id IS NULL;

UPDATE account_limit al
SET template_public_id = tt.public_id
FROM transaction_template tt
WHERE al.template_name = tt.name
  AND al.merchant_id = tt.merchant_id
  AND al.merchant_id IS NOT NULL
  AND al.template_public_id IS NULL;

UPDATE account_limit al
SET template_public_id = tt.public_id
FROM transaction_template tt
WHERE al.template_name = tt.name
  AND tt.merchant_id IS NULL
  AND al.merchant_id IS NOT NULL
  AND al.template_public_id IS NULL;

DROP INDEX IF EXISTS idx_template_limit_system;
DROP INDEX IF EXISTS idx_template_limit_merchant;
DROP INDEX IF EXISTS idx_template_limit_lookup;
DROP INDEX IF EXISTS idx_template_limit_merchant_id;
ALTER TABLE account_limit DROP CONSTRAINT IF EXISTS uk_template_limit;

ALTER TABLE account_limit DROP COLUMN IF EXISTS template_name;

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_limit_system
    ON account_limit (template_public_id, profile_id, currency) WHERE merchant_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_limit_merchant
    ON account_limit (merchant_id, template_public_id, profile_id, currency) WHERE merchant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_account_limit_merchant_id
    ON account_limit (merchant_id) WHERE merchant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_account_limit_lookup
    ON account_limit (template_public_id, profile_id, currency, valid_from, valid_to);

ALTER TABLE transaction ADD COLUMN IF NOT EXISTS template_public_id uuid;

UPDATE transaction t
SET template_public_id = tt.public_id
FROM transaction_template tt
WHERE t.type = tt.name
  AND t.merchant_id = tt.merchant_id
  AND t.template_public_id IS NULL;

UPDATE transaction t
SET template_public_id = tt.public_id
FROM transaction_template tt
WHERE t.type = tt.name
  AND tt.merchant_id IS NULL
  AND t.template_public_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_transaction_template_public_id
    ON transaction (template_public_id) WHERE template_public_id IS NOT NULL;
