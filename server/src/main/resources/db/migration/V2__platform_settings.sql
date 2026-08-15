-- Platform-wide settings editable from the admin console.
-- Key/value shaped so adding a setting needs no further migration.

CREATE TABLE IF NOT EXISTS platform_settings (
    setting_key   VARCHAR(100) NOT NULL PRIMARY KEY,
    setting_value TEXT,
    updated_by    BIGINT,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_platform_settings_user FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);
