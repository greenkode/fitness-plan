ALTER TABLE billing_plan
    ALTER COLUMN platform_fee_cents TYPE NUMERIC(19,3),
    ALTER COLUMN per_account_fee_cents TYPE NUMERIC(19,3),
    ALTER COLUMN per_transaction_fee_cents TYPE NUMERIC(19,3);

ALTER TABLE billing_plan RENAME COLUMN platform_fee_cents TO platform_fee_amount;
ALTER TABLE billing_plan RENAME COLUMN per_account_fee_cents TO per_account_fee_amount;
ALTER TABLE billing_plan RENAME COLUMN per_transaction_fee_cents TO per_transaction_fee_amount;

ALTER TABLE billing_plan ADD COLUMN IF NOT EXISTS max_charge_amount NUMERIC(19,3);
