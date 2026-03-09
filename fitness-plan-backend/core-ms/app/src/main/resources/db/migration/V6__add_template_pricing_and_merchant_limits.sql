ALTER TABLE template_limit ADD COLUMN IF NOT EXISTS merchant_id uuid;
ALTER TABLE template_limit ADD COLUMN IF NOT EXISTS public_id uuid DEFAULT gen_random_uuid();

UPDATE template_limit SET public_id = gen_random_uuid() WHERE public_id IS NULL;

ALTER TABLE template_limit DROP CONSTRAINT IF EXISTS uk_template_limit;
CREATE UNIQUE INDEX IF NOT EXISTS idx_template_limit_system
    ON template_limit(template_name, profile_id, currency) WHERE merchant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_template_limit_merchant
    ON template_limit(merchant_id, template_name, profile_id, currency) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_template_limit_merchant_id
    ON template_limit(merchant_id) WHERE merchant_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS template_pricing (
    id               bigserial PRIMARY KEY,
    public_id        uuid NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    merchant_id      uuid NOT NULL,
    template_name    varchar(100) NOT NULL,
    currency         varchar(3) NOT NULL,
    valid_from       timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to         timestamp with time zone,
    is_active        boolean NOT NULL DEFAULT true,
    created_at       timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by       varchar(255),
    last_modified_by varchar(255),
    version          bigint DEFAULT 0,
    CONSTRAINT uk_template_pricing UNIQUE (merchant_id, template_name, currency, valid_from)
);

CREATE INDEX IF NOT EXISTS idx_template_pricing_merchant ON template_pricing(merchant_id);
CREATE INDEX IF NOT EXISTS idx_template_pricing_lookup
    ON template_pricing(merchant_id, template_name, currency, is_active);

CREATE TABLE IF NOT EXISTS template_pricing_tier (
    id               bigserial PRIMARY KEY,
    pricing_id       bigint NOT NULL REFERENCES template_pricing(id) ON DELETE CASCADE,
    pricing_type     varchar(50) NOT NULL,
    calculation_type varchar(50) NOT NULL,
    value            numeric(19, 4) NOT NULL,
    min_value        numeric(19, 4),
    max_value        numeric(19, 4),
    expression       text,
    sequence         int NOT NULL DEFAULT 0,
    created_at       timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by       varchar(255),
    last_modified_by varchar(255),
    version          bigint DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_pricing_tier_pricing ON template_pricing_tier(pricing_id);
