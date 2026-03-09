DROP MATERIALIZED VIEW IF EXISTS mv_account_summary_stats;

CREATE MATERIALIZED VIEW mv_account_summary_stats AS
SELECT
    a.merchant_id,
    a.type,
    a.status,
    COUNT(*) AS account_count
FROM account a
WHERE a.parent_id IS NOT NULL
AND a.type NOT IN ('EQUITY', 'CASH', 'FEE', 'EXPENSE', 'REVENUE', 'LOCK', 'CONVERSION', 'POOL', 'BRIDGE_ASSETS', 'BRIDGE_LIABILITIES')
GROUP BY a.merchant_id, a.type, a.status;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_account_summary_unique
    ON mv_account_summary_stats (merchant_id, type, status);

CREATE INDEX IF NOT EXISTS idx_mv_account_summary_merchant
    ON mv_account_summary_stats (merchant_id);
