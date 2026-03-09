DROP MATERIALIZED VIEW IF EXISTS mv_daily_export_stats;

CREATE MATERIALIZED VIEW mv_daily_export_stats AS
SELECT
    er.merchant_id,
    DATE_TRUNC('day', er.created_at) AS stat_date,
    COUNT(*) AS export_count,
    COUNT(*) FILTER (WHERE er.status = 'COMPLETED') AS completed_count,
    COUNT(*) FILTER (WHERE er.status = 'FAILED') AS failed_count,
    COUNT(*) FILTER (WHERE er.status = 'PROCESSING') AS processing_count,
    COALESCE(SUM(er.file_size_bytes), 0) AS total_bytes,
    er.format
FROM export_record er
GROUP BY er.merchant_id, DATE_TRUNC('day', er.created_at), er.format;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_daily_export_stats_unique
    ON mv_daily_export_stats (merchant_id, stat_date, format);

CREATE INDEX IF NOT EXISTS idx_mv_daily_export_stats_merchant_date
    ON mv_daily_export_stats (merchant_id, stat_date);
