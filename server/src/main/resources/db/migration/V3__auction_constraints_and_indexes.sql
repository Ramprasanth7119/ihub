-- Integrity and performance guarantees the original schema left to application code.

-- An idea must never have two live auctions. AuctionDao.auctionExistsForIdea checks
-- this, but two concurrent creates can both pass that check. A generated column that
-- is NULL for terminal auctions, plus a unique index (MySQL ignores NULLs in unique
-- indexes), makes the database itself reject the second insert.
ALTER TABLE auctions
    ADD COLUMN live_idea_id BIGINT
    GENERATED ALWAYS AS (IF(status IN ('SCHEDULED', 'ACTIVE'), idea_id, NULL)) STORED;

CREATE UNIQUE INDEX uq_auctions_live_idea ON auctions (live_idea_id);

-- Supports the scheduler sweeps (status + time window) without scanning the table.
CREATE INDEX idx_auctions_status_start ON auctions (status, start_time);
CREATE INDEX idx_auctions_status_end ON auctions (status, end_time);

-- Supports the "has this auction already been warned?" NOT EXISTS lookup.
CREATE INDEX idx_auction_events_auction_type ON auction_events (auction_id, event_type);

-- Supports the "my bids" listing, which orders an investor's bids by recency.
CREATE INDEX idx_bids_investor_created ON bids (investor_id, created_at);
