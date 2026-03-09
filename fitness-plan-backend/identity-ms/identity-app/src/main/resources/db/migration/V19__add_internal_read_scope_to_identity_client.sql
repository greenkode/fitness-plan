INSERT INTO oauth_client_scope (registered_client_id, scope_id)
SELECT 'f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, id FROM oauth_scope WHERE name = 'internal:read'
ON CONFLICT DO NOTHING;
