CREATE OR REPLACE FUNCTION refresh_all_materialized_views()
RETURNS TABLE (
    view_name TEXT,
    refresh_duration_seconds NUMERIC,
    status TEXT
) AS $$
DECLARE
    v_start_time TIMESTAMP;
    v_end_time TIMESTAMP;
    v_view RECORD;
BEGIN
    FOR v_view IN
        SELECT matviewname::TEXT AS name
        FROM pg_matviews
        WHERE schemaname = 'core'
        ORDER BY matviewname
    LOOP
        v_start_time := CURRENT_TIMESTAMP;
        BEGIN
            EXECUTE format('REFRESH MATERIALIZED VIEW CONCURRENTLY %I.%I', 'core', v_view.name);
            v_end_time := CURRENT_TIMESTAMP;
            view_name := v_view.name;
            refresh_duration_seconds := EXTRACT(EPOCH FROM (v_end_time - v_start_time));
            status := 'SUCCESS';
            RETURN NEXT;
        EXCEPTION WHEN OTHERS THEN
            view_name := v_view.name;
            refresh_duration_seconds := NULL;
            status := 'FAILED: ' || SQLERRM;
            RETURN NEXT;
        END;
    END LOOP;

    RETURN;
END;
$$ LANGUAGE plpgsql;
