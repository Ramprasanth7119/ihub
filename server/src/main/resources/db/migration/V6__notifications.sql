CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    message         TEXT         NOT NULL,
    reference_type  VARCHAR(50),
    reference_id    BIGINT,
    read_flag       BOOLEAN      DEFAULT FALSE,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notifications_user (user_id),
    INDEX idx_notifications_user_read (user_id, read_flag),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS email_outbox (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(150) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(20)  DEFAULT 'PENDING',
    attempts        INT          DEFAULT 0,
    last_error      VARCHAR(500),
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    sent_at         DATETIME,
    INDEX idx_email_outbox_status (status)
);
