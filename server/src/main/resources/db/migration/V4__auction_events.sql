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
