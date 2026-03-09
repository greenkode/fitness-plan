ALTER TABLE account_profile ADD COLUMN IF NOT EXISTS profile_type VARCHAR(20) NOT NULL DEFAULT 'DEFAULT';
ALTER TABLE account_profile ADD COLUMN IF NOT EXISTS source_profile_id UUID;

UPDATE account_profile SET profile_type = 'INTERNAL' WHERE name = 'Chart Import Profile' AND merchant_id IS NULL;
UPDATE account_profile SET profile_type = 'DEFAULT' WHERE merchant_id IS NULL AND profile_type != 'INTERNAL';
UPDATE account_profile SET profile_type = 'MERCHANT' WHERE merchant_id IS NOT NULL;
