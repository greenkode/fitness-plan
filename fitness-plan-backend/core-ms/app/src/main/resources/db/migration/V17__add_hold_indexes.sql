CREATE INDEX IF NOT EXISTS idx_transaction_type_status
    ON transaction (type, status);

CREATE INDEX IF NOT EXISTS idx_transaction_property_hold_expiry
    ON transaction_property (name, value)
    WHERE name = 'HOLD_EXPIRES_AT';
