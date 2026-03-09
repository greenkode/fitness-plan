ALTER TABLE transaction_entry
    ADD COLUMN IF NOT EXISTS posting_status VARCHAR(10) NOT NULL DEFAULT 'POSTED';

ALTER TABLE transaction_entry
    ADD COLUMN IF NOT EXISTS running_balance NUMERIC(14,2);

CREATE INDEX IF NOT EXISTS idx_entry_posting_status
    ON transaction_entry (posting_status);

CREATE INDEX IF NOT EXISTS idx_entry_account_posting_status
    ON transaction_entry (account, posting_status);

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS resolution_policy VARCHAR(20) NOT NULL DEFAULT 'AUTO_REVERSE';

CREATE INDEX IF NOT EXISTS idx_transaction_expires_at
    ON transaction (expires_at)
    WHERE expires_at IS NOT NULL;

ALTER TABLE transaction DROP COLUMN IF EXISTS sender_account_id;

ALTER TABLE transaction DROP COLUMN IF EXISTS recipient_account_id;

ALTER TABLE transaction DROP COLUMN IF EXISTS sender_running_balance;

ALTER TABLE transaction DROP COLUMN IF EXISTS recipient_running_balance;
