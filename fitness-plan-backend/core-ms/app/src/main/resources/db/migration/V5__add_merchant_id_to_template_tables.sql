ALTER TABLE chart_of_accounts_template ADD COLUMN IF NOT EXISTS merchant_id uuid;
ALTER TABLE chart_of_accounts_template ADD COLUMN IF NOT EXISTS source_template_id uuid;

ALTER TABLE chart_of_accounts_template DROP CONSTRAINT IF EXISTS chart_of_accounts_template_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_coa_template_default_name
    ON chart_of_accounts_template(name) WHERE merchant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_coa_template_merchant_name
    ON chart_of_accounts_template(merchant_id, name) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_coa_template_merchant_id
    ON chart_of_accounts_template(merchant_id) WHERE merchant_id IS NOT NULL;

ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS merchant_id uuid;
ALTER TABLE transaction_template ADD COLUMN IF NOT EXISTS source_template_id uuid;

ALTER TABLE transaction_template DROP CONSTRAINT IF EXISTS transaction_template_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_txn_template_default_name
    ON transaction_template(name) WHERE merchant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_txn_template_merchant_name
    ON transaction_template(merchant_id, name) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_txn_template_merchant_id
    ON transaction_template(merchant_id) WHERE merchant_id IS NOT NULL;
