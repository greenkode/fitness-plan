INSERT INTO navigation_item (key, label, icon, path, parent_id, sort_order, platform) VALUES
    ('main', 'Main', NULL, NULL, NULL, 0, 'WEB'),
    ('accounting', 'Accounting', NULL, NULL, NULL, 1, 'WEB'),
    ('reports', 'Reports', NULL, NULL, NULL, 2, 'WEB'),
    ('settings', 'Settings', 'mdi-cog', NULL, NULL, 3, 'WEB')
ON CONFLICT (platform, key) DO NOTHING;

INSERT INTO navigation_item (key, label, icon, path, parent_id, sort_order, platform) VALUES
    ('dashboard', 'Dashboard', 'mdi-view-dashboard', '/dashboard', (SELECT id FROM navigation_item WHERE key = 'main'), 0, 'WEB'),
    ('customers', 'Customers', 'mdi-account-group', '/customers', (SELECT id FROM navigation_item WHERE key = 'main'), 1, 'WEB'),
    ('accounts', 'Accounts', 'mdi-wallet', '/accounts', (SELECT id FROM navigation_item WHERE key = 'main'), 2, 'WEB'),
    ('transaction-history', 'Transactions', 'mdi-swap-horizontal', '/transaction-history', (SELECT id FROM navigation_item WHERE key = 'main'), 3, 'WEB')
ON CONFLICT (platform, key) DO NOTHING;

INSERT INTO navigation_item (key, label, icon, path, parent_id, sort_order, platform) VALUES
    ('transaction-templates', 'Transaction Templates', 'mdi-swap-horizontal', '/transactions', (SELECT id FROM navigation_item WHERE key = 'accounting'), 0, 'WEB'),
    ('campaigns', 'Campaigns', 'mdi-bullhorn-variant', '/campaigns', (SELECT id FROM navigation_item WHERE key = 'accounting'), 1, 'WEB'),
    ('limits', 'Limits', 'mdi-shield-check', '/limits', (SELECT id FROM navigation_item WHERE key = 'accounting'), 2, 'WEB'),
    ('profiles', 'Profiles', 'mdi-account-group', '/profiles', (SELECT id FROM navigation_item WHERE key = 'accounting'), 3, 'WEB'),
    ('journal-entries', 'Journal Entries', 'mdi-notebook-outline', '/journal', (SELECT id FROM navigation_item WHERE key = 'accounting'), 4, 'WEB')
ON CONFLICT (platform, key) DO NOTHING;

INSERT INTO navigation_item (key, label, icon, path, parent_id, sort_order, platform) VALUES
    ('chart-of-accounts', 'Chart of Accounts', 'mdi-book-open-page-variant', '/reports/chart-of-accounts', (SELECT id FROM navigation_item WHERE key = 'reports'), 0, 'WEB'),
    ('trial-balance', 'Trial Balance', 'mdi-table-check', '/reports/trial-balance', (SELECT id FROM navigation_item WHERE key = 'reports'), 1, 'WEB'),
    ('balance-sheet', 'Balance Sheet', 'mdi-scale-balance', '/reports/balance-sheet', (SELECT id FROM navigation_item WHERE key = 'reports'), 2, 'WEB'),
    ('income-statement', 'Income Statement', 'mdi-chart-line', '/reports/income-statement', (SELECT id FROM navigation_item WHERE key = 'reports'), 3, 'WEB')
ON CONFLICT (platform, key) DO NOTHING;

INSERT INTO navigation_item (key, label, icon, path, parent_id, sort_order, platform) VALUES
    ('organization', 'Organization', NULL, '/settings/organization', (SELECT id FROM navigation_item WHERE key = 'settings'), 0, 'WEB'),
    ('profile', 'Profile', NULL, '/settings/profile', (SELECT id FROM navigation_item WHERE key = 'settings'), 1, 'WEB'),
    ('team', 'Team', NULL, '/settings/team', (SELECT id FROM navigation_item WHERE key = 'settings'), 2, 'WEB')
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
WHERE ni.key IN ('dashboard', 'customers', 'accounts', 'transaction-history', 'journal-entries',
                 'chart-of-accounts', 'trial-balance', 'balance-sheet', 'income-statement',
                 'organization', 'profile')
ON CONFLICT DO NOTHING;

INSERT INTO navigation_item_role (navigation_item_id, role)
SELECT ni.id, r.role
FROM navigation_item ni
CROSS JOIN (
    VALUES
        ('ROLE_MERCHANT_ADMIN'),
        ('ROLE_MERCHANT_SUPER_ADMIN')
) AS r(role)
WHERE ni.key IN ('transaction-templates', 'campaigns', 'limits', 'profiles')
ON CONFLICT DO NOTHING;

INSERT INTO navigation_item_role (navigation_item_id, role)
SELECT ni.id, 'ROLE_MERCHANT_SUPER_ADMIN'
FROM navigation_item ni
WHERE ni.key = 'team'
ON CONFLICT DO NOTHING;
