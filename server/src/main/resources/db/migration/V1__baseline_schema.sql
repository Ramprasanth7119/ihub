-- iHub baseline schema (inferred from existing DAO layer)

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    verified    BOOLEAN      DEFAULT FALSE,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_role (role)
);

CREATE TABLE IF NOT EXISTS ideas (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id  BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    category    VARCHAR(50),
    base_price  DECIMAL(12,2),
    status      VARCHAR(20)  DEFAULT 'OPEN',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ideas_creator (creator_id),
    INDEX idx_ideas_category (category),
    INDEX idx_ideas_status (status),
    CONSTRAINT fk_ideas_creator FOREIGN KEY (creator_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS auctions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    idea_id     BIGINT       NOT NULL,
    start_time  DATETIME     NOT NULL,
    end_time    DATETIME     NOT NULL,
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

CREATE TABLE IF NOT EXISTS auction_winners (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id  BIGINT       NOT NULL UNIQUE,
    winner_id   BIGINT       NOT NULL,
    winning_bid DECIMAL(12,2) NOT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_winners_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
    CONSTRAINT fk_winners_user FOREIGN KEY (winner_id) REFERENCES users(id)
);
