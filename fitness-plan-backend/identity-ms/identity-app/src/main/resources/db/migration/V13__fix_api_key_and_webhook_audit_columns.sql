ALTER TABLE api_key ALTER COLUMN created_at DROP NOT NULL;

ALTER TABLE api_key RENAME COLUMN updated_at TO last_modified_at;
ALTER TABLE api_key ALTER COLUMN last_modified_at DROP NOT NULL;

ALTER TABLE api_key ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255);

ALTER TABLE webhook_config ALTER COLUMN created_at DROP NOT NULL;

ALTER TABLE webhook_config RENAME COLUMN updated_at TO last_modified_at;
ALTER TABLE webhook_config ALTER COLUMN last_modified_at DROP NOT NULL;

ALTER TABLE webhook_config ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE webhook_config ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE webhook_config ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255);
