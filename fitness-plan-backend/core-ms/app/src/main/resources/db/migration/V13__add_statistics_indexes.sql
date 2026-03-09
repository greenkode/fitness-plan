CREATE INDEX IF NOT EXISTS idx_transaction_journal_status_created
    ON transaction (journal_id, status, created_at);

CREATE INDEX IF NOT EXISTS idx_account_owner_currency
    ON account (owner_id, currency);
