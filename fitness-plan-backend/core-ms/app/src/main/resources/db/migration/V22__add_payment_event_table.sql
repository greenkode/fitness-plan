CREATE TABLE IF NOT EXISTS payment_event (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    external_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    invoice_public_id UUID,
    amount_cents BIGINT,
    currency VARCHAR(3),
    raw_payload TEXT,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    last_modified_at TIMESTAMPTZ,
    last_modified_by VARCHAR(255) DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_event_external_id ON payment_event(external_event_id);
CREATE INDEX IF NOT EXISTS idx_payment_event_merchant ON payment_event(merchant_id);
CREATE INDEX IF NOT EXISTS idx_payment_event_invoice ON payment_event(invoice_public_id);
