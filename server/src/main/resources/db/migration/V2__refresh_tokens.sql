CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  DATETIME     NOT NULL,
    revoked     BOOLEAN      DEFAULT FALSE,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refresh_user (user_id),
    INDEX idx_refresh_expires (expires_at),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
