INSERT INTO navigation_item (key, label, icon, path, parent_id, sort_order, platform) VALUES
    ('analytics', 'Analytics', 'mdi-chart-bar', NULL, NULL, 2, 'WEB'),
    ('billing', 'Billing', 'mdi-receipt-text', NULL, NULL, 4, 'WEB')
ON CONFLICT (platform, key) DO NOTHING;

UPDATE navigation_item SET sort_order = 3 WHERE key = 'reports' AND platform = 'WEB';
UPDATE navigation_item SET sort_order = 5 WHERE key = 'settings' AND platform = 'WEB';

INSERT INTO navigation_item (key, label, icon, path, parent_id, sort_order, platform) VALUES
    ('performance', 'Performance', 'mdi-speedometer', '/analytics/performance', (SELECT id FROM navigation_item WHERE key = 'analytics' AND platform = 'WEB'), 0, 'WEB')
ON CONFLICT (platform, key) DO NOTHING;

INSERT INTO navigation_item (key, label, icon, path, parent_id, sort_order, platform) VALUES
    ('billing-usage', 'Billing & Usage', 'mdi-credit-card-outline', '/billing', (SELECT id FROM navigation_item WHERE key = 'billing' AND platform = 'WEB'), 0, 'WEB')
ON CONFLICT (platform, key) DO NOTHING;

INSERT INTO navigation_item (key, label, icon, path, parent_id, sort_order, platform) VALUES
    ('api-keys', 'API Keys', 'mdi-key-variant', '/settings/api-keys', (SELECT id FROM navigation_item WHERE key = 'settings' AND platform = 'WEB'), 3, 'WEB'),
    ('webhooks', 'Webhooks', 'mdi-webhook', '/settings/webhooks', (SELECT id FROM navigation_item WHERE key = 'settings' AND platform = 'WEB'), 4, 'WEB')
ON CONFLICT (platform, key) DO NOTHING;

INSERT INTO navigation_item_role (navigation_item_id, role)
SELECT ni.id, r.role
FROM navigation_item ni
CROSS JOIN (
    VALUES
        ('ROLE_MERCHANT_USER'),
        ('ROLE_MERCHANT_FINANCE_ADMIN'),
        ('ROLE_MERCHANT_ADMIN'),
        ('ROLE_MERCHANT_SUPER_ADMIN')
) AS r(role)
WHERE ni.key IN ('performance', 'billing-usage')
ON CONFLICT DO NOTHING;

INSERT INTO navigation_item_role (navigation_item_id, role)
SELECT ni.id, r.role
FROM navigation_item ni
CROSS JOIN (
    VALUES
        ('ROLE_MERCHANT_ADMIN'),
        ('ROLE_MERCHANT_SUPER_ADMIN')
) AS r(role)
WHERE ni.key IN ('api-keys', 'webhooks')
ON CONFLICT DO NOTHING;
