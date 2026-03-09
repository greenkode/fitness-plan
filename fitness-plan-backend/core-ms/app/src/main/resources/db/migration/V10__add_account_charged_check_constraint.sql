DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_account_charged'
    ) THEN
        ALTER TABLE transaction_template
            ADD CONSTRAINT chk_account_charged
            CHECK (account_charged IN ('SENDER', 'RECIPIENT', 'BOTH', 'NONE'));
    END IF;
END $$;
