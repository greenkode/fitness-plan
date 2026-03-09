INSERT INTO account_profile (name, description, public_id, skip_limits, profile_type, created_by, created_at, last_modified_by, last_modified_at, version)
VALUES
    ('TIER_ZERO', 'Tier Zero - Basic verification', gen_random_uuid(), false, 'DEFAULT', 'system', now(), 'system', now(), 0),
    ('TIER_ONE', 'Tier One - Standard verification', gen_random_uuid(), false, 'DEFAULT', 'system', now(), 'system', now(), 0),
    ('TIER_TWO', 'Tier Two - Enhanced verification', gen_random_uuid(), false, 'DEFAULT', 'system', now(), 'system', now(), 0),
    ('TIER_THREE', 'Tier Three - Full verification', gen_random_uuid(), true, 'DEFAULT', 'system', now(), 'system', now(), 0),
    ('Chart Import Profile', 'Profile for chart imported accounts', gen_random_uuid(), true, 'INTERNAL', 'system', now(), 'system', now(), 0)
ON CONFLICT (name) WHERE merchant_id IS NULL DO NOTHING;
