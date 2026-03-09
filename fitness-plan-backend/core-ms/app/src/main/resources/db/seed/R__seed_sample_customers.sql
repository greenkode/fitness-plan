DO $$
DECLARE
    sample_merchant UUID := '00000000-0000-0000-0000-000000000001';
BEGIN
    IF NOT EXISTS (SELECT 1 FROM customer WHERE merchant_id = sample_merchant LIMIT 1) THEN

        INSERT INTO customer (public_id, merchant_id, external_id, status, profile, created_at, version)
        VALUES
            ('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', sample_merchant, 'CUS-001', 'ACTIVE', 'PREMIUM', now() - interval '90 days', 0),
            ('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', sample_merchant, 'CUS-002', 'ACTIVE', 'STANDARD', now() - interval '80 days', 0),
            ('c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f', sample_merchant, 'CUS-003', 'ACTIVE', 'STANDARD', now() - interval '15 days', 0),
            ('d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a', sample_merchant, 'CUS-004', 'ACTIVE', 'VIP', now() - interval '120 days', 0),
            ('e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b', sample_merchant, 'CUS-005', 'SUSPENDED', NULL, now() - interval '60 days', 0),
            ('f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c', sample_merchant, 'CUS-006', 'ACTIVE', 'PREMIUM', now() - interval '45 days', 0),
            ('a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d', sample_merchant, 'CUS-007', 'INACTIVE', 'STANDARD', now() - interval '200 days', 0),
            ('b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e', sample_merchant, 'CUS-008', 'ACTIVE', 'STANDARD', now() - interval '30 days', 0),
            ('c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f', sample_merchant, 'CUS-009', 'ACTIVE', NULL, now() - interval '5 days', 0),
            ('d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a', sample_merchant, 'CUS-010', 'ACTIVE', 'PREMIUM', now() - interval '70 days', 0),
            ('e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', sample_merchant, 'CUS-011', 'ACTIVE', NULL, now() - interval '3 days', 0),
            ('f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c', sample_merchant, 'CUS-012', 'ACTIVE', 'VIP', now() - interval '100 days', 0),
            ('a3b4c5d6-e7f8-4a9b-0c1d-2e3f4a5b6c7d', sample_merchant, 'CUS-013', 'ACTIVE', 'PREMIUM', now() - interval '150 days', 0),
            ('b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e', sample_merchant, 'CUS-014', 'ACTIVE', 'STANDARD', now() - interval '7 days', 0),
            ('c5d6e7f8-a9b0-4c1d-2e3f-4a5b6c7d8e9f', sample_merchant, 'CUS-015', 'CLOSED', 'STANDARD', now() - interval '300 days', 0);

    END IF;
END $$;
