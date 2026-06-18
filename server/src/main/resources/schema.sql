-- Run on startup (CREATE IF NOT EXISTS is idempotent)

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    verified    BOOLEAN      DEFAULT FALSE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_role (role)
);

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ideas (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id  BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    category    VARCHAR(50),
    base_price  DECIMAL(12,2),
    max_budget  DECIMAL(12,2),
    status      VARCHAR(20)  DEFAULT 'DRAFT',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ideas_creator (creator_id),
    INDEX idx_ideas_category (category),
    INDEX idx_ideas_status (status),
    INDEX idx_ideas_budget (base_price, max_budget),
    CONSTRAINT fk_ideas_creator FOREIGN KEY (creator_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS tags (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS idea_tags (
    idea_id     BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (idea_id, tag_id),
    CONSTRAINT fk_idea_tags_idea FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
    CONSTRAINT fk_idea_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auctions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    idea_id     BIGINT       NOT NULL,
    start_time  DATETIME     NOT NULL,
    end_time    DATETIME     NOT NULL,
    min_bid_increment DECIMAL(12,2) NOT NULL DEFAULT 100.00,
    status      VARCHAR(20)  DEFAULT 'SCHEDULED',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auctions_idea (idea_id),
    INDEX idx_auctions_status (status),
    INDEX idx_auctions_times (start_time, end_time),
    CONSTRAINT fk_auctions_idea FOREIGN KEY (idea_id) REFERENCES ideas(id)
);

CREATE TABLE IF NOT EXISTS bids (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id  BIGINT       NOT NULL,
    investor_id BIGINT       NOT NULL,
    bid_amount  DECIMAL(12,2) NOT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bids_auction (auction_id),
    INDEX idx_bids_investor (investor_id),
    INDEX idx_bids_auction_amount (auction_id, bid_amount DESC),
    CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
    CONSTRAINT fk_bids_investor FOREIGN KEY (investor_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS auction_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id  BIGINT       NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    details     VARCHAR(500),
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auction_events_auction (auction_id),
    INDEX idx_auction_events_type (event_type),
    CONSTRAINT fk_auction_events_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auction_winners (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id  BIGINT       NOT NULL UNIQUE,
    winner_id   BIGINT       NOT NULL,
    winning_bid DECIMAL(12,2) NOT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_winners_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
    CONSTRAINT fk_winners_user FOREIGN KEY (winner_id) REFERENCES users(id)
);

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
    INDEX idx_notifications_created (created_at),
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
    INDEX idx_email_outbox_status (status),
    INDEX idx_email_outbox_created (created_at)
);

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

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id        BIGINT       NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       BIGINT,
    details         TEXT,
    ip_address      VARCHAR(45),
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_admin (admin_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_created (created_at),
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS auction_settings (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    idea_id             BIGINT       NOT NULL UNIQUE,
    min_bid             DECIMAL(12,2),
    reserve_price       DECIMAL(12,2),
    description         TEXT,
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_auction_settings_idea FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE
);
