CREATE TABLE IF NOT EXISTS oauth_user_setting (
    user_id UUID NOT NULL,
    setting_name VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    PRIMARY KEY (user_id, setting_name),
    CONSTRAINT fk_user_setting_user FOREIGN KEY (user_id) REFERENCES oauth_user(id) ON DELETE CASCADE
);
