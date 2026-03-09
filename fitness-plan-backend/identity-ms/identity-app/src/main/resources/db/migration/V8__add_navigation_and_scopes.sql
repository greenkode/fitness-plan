INSERT INTO oauth_scope (name) VALUES ('krachtix:read'), ('krachtix:write'), ('krachtix:admin')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS navigation_item (
    id SERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    platform VARCHAR(50) NOT NULL DEFAULT 'WEB',
    key VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    icon VARCHAR(100),
    path VARCHAR(255),
    parent_id INT REFERENCES navigation_item(id),
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(platform, key)
);

CREATE TABLE IF NOT EXISTS navigation_item_role (
    navigation_item_id INT NOT NULL REFERENCES navigation_item(id) ON DELETE CASCADE,
    role VARCHAR(100) NOT NULL,
    PRIMARY KEY (navigation_item_id, role)
);

CREATE INDEX IF NOT EXISTS idx_nav_item_platform_active ON navigation_item(platform, active);
CREATE INDEX IF NOT EXISTS idx_nav_item_parent ON navigation_item(parent_id);
CREATE INDEX IF NOT EXISTS idx_nav_item_role_role ON navigation_item_role(role);
