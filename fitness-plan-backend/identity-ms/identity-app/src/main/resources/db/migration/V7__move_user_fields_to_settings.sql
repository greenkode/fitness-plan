INSERT INTO oauth_user_setting (user_id, setting_name, setting_value)
SELECT id, 'INVITATION_STATUS', invitation_status::text
FROM oauth_user
WHERE invitation_status = true
ON CONFLICT (user_id, setting_name) DO NOTHING;

INSERT INTO oauth_user_setting (user_id, setting_name, setting_value)
SELECT id, 'LOCALE', locale
FROM oauth_user
WHERE locale != 'en'
ON CONFLICT (user_id, setting_name) DO NOTHING;

INSERT INTO oauth_user_setting (user_id, setting_name, setting_value)
SELECT id, 'ENVIRONMENT_PREFERENCE', environment_preference
FROM oauth_user
WHERE environment_preference != 'SANDBOX'
ON CONFLICT (user_id, setting_name) DO NOTHING;

INSERT INTO oauth_user_setting (user_id, setting_name, setting_value)
SELECT id, 'ENVIRONMENT_LAST_SWITCHED_AT', environment_last_switched_at::text
FROM oauth_user
WHERE environment_last_switched_at IS NOT NULL
ON CONFLICT (user_id, setting_name) DO NOTHING;

INSERT INTO oauth_user_setting (user_id, setting_name, setting_value)
SELECT id, 'REGISTRATION_COMPLETE', registration_complete::text
FROM oauth_user
WHERE registration_complete = true
ON CONFLICT (user_id, setting_name) DO NOTHING;

ALTER TABLE oauth_user DROP COLUMN IF EXISTS invitation_status;
ALTER TABLE oauth_user DROP COLUMN IF EXISTS locale;
ALTER TABLE oauth_user DROP COLUMN IF EXISTS environment_preference;
ALTER TABLE oauth_user DROP COLUMN IF EXISTS environment_last_switched_at;
ALTER TABLE oauth_user DROP COLUMN IF EXISTS registration_complete;
