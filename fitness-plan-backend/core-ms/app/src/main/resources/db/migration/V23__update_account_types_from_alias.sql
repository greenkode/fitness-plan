UPDATE account
SET type = CASE alias
    WHEN 'cash' THEN 'CASH'
    WHEN 'cash-and-bank' THEN 'CASH'
    WHEN 'receivables' THEN 'CASH'
    WHEN 'transaction-fees' THEN 'FEE'
    WHEN 'commission-income' THEN 'FEE'
    WHEN 'interest-income' THEN 'REVENUE'
    WHEN 'payment-processor-fees' THEN 'EXPENSE'
    WHEN 'bank-charges' THEN 'EXPENSE'
    WHEN 'operational-costs' THEN 'EXPENSE'
    WHEN 'forex-gains-losses' THEN 'CONVERSION'
    ELSE type
END
WHERE type = 'EQUITY'
  AND subclass = 'FINAL'
  AND alias IN (
    'cash', 'cash-and-bank', 'receivables',
    'transaction-fees', 'commission-income',
    'interest-income',
    'payment-processor-fees', 'bank-charges', 'operational-costs',
    'forex-gains-losses'
  );

CREATE INDEX IF NOT EXISTS idx_account_root_type ON account (root_id, type);
