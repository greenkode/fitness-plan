DROP MATERIALIZED VIEW IF EXISTS mv_daily_transaction_stats;

CREATE MATERIALIZED VIEW mv_daily_transaction_stats AS
SELECT
    t.merchant_id,
    t.journal_id,
    DATE_TRUNC('day', t.created_at) AS stat_date,
    COUNT(*) AS transaction_count,
    COALESCE(SUM(t.sender_amount), 0) AS transaction_value
FROM transaction t
WHERE t.status = 'COMPLETED'
GROUP BY t.merchant_id, t.journal_id, DATE_TRUNC('day', t.created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_daily_txn_stats_unique
    ON mv_daily_transaction_stats (merchant_id, journal_id, stat_date);

CREATE INDEX IF NOT EXISTS idx_mv_daily_txn_stats_merchant_date
    ON mv_daily_transaction_stats (merchant_id, stat_date);

CREATE INDEX IF NOT EXISTS idx_mv_daily_txn_stats_journal_date
    ON mv_daily_transaction_stats (journal_id, stat_date);
