CREATE TABLE IF NOT EXISTS organization_property (
    organization_id UUID NOT NULL,
    property_name VARCHAR(100) NOT NULL,
    property_value TEXT NOT NULL,
    PRIMARY KEY (organization_id, property_name),
    CONSTRAINT fk_org_property_org FOREIGN KEY (organization_id)
        REFERENCES organization(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_org_property_org_id ON organization_property(organization_id);

INSERT INTO organization_property (organization_id, property_name, property_value)
SELECT registered_client_id, setting_name, setting_value
FROM oauth_client_setting
WHERE setting_name NOT IN ('REQUIRE_AUTHORIZATION_CONSENT', 'REQUIRE_PROOF_KEY')
ON CONFLICT DO NOTHING;

DELETE FROM oauth_client_setting
WHERE setting_name NOT IN ('REQUIRE_AUTHORIZATION_CONSENT', 'REQUIRE_PROOF_KEY');
