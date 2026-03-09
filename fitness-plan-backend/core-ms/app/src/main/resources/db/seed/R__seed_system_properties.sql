INSERT INTO system_property (name, scope, value, created_at, last_modified_at, created_by, last_modified_by, version)
VALUES ('CHART_IMPORT_MAX_DEPTH', 'SYSTEM', '5', now(), now(), 'system', 'system', 0)
ON CONFLICT (name, scope) DO UPDATE SET value = EXCLUDED.value;

INSERT INTO system_property (name, scope, value, created_at, last_modified_at, created_by, last_modified_by, version)
VALUES ('CHART_IMPORT_MAX_FINAL_ACCOUNTS', 'SYSTEM', '50', now(), now(), 'system', 'system', 0)
ON CONFLICT (name, scope) DO UPDATE SET value = EXCLUDED.value;

INSERT INTO system_property (name, scope, value, created_at, last_modified_at, created_by, last_modified_by, version)
VALUES ('CHART_IMPORT_MAX_TOTAL_ACCOUNTS', 'SYSTEM', '200', now(), now(), 'system', 'system', 0)
ON CONFLICT (name, scope) DO UPDATE SET value = EXCLUDED.value;

INSERT INTO system_property (name, scope, value, created_at, last_modified_at, created_by, last_modified_by, version)
VALUES ('CHART_IMPORT_CUSTOM_ASYNC', 'SYSTEM', 'true', now(), now(), 'system', 'system', 0)
ON CONFLICT (name, scope) DO UPDATE SET value = EXCLUDED.value;
