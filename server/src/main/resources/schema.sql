-- iHub Database Schema
-- Run this script to initialize the ihub_db database

CREATE DATABASE IF NOT EXISTS ihub;
USE ihub;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('CREATOR', 'INVESTOR') NOT NULL,
    verified    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Ideas table
CREATE TABLE IF NOT EXISTS ideas (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id  BIGINT NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    category    VARCHAR(100),
    base_price  DOUBLE,
    status      VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_idea_creator FOREIGN KEY (creator_id) REFERENCES users (id)
);

-- Auctions table
CREATE TABLE IF NOT EXISTS auctions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    idea_id     BIGINT NOT NULL UNIQUE,
    start_time  DATETIME NOT NULL,
    end_time    DATETIME NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auction_idea FOREIGN KEY (idea_id) REFERENCES ideas (id)
);

-- Bids table
CREATE TABLE IF NOT EXISTS bids (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id  BIGINT NOT NULL,
    investor_id BIGINT NOT NULL,
    bid_amount  DOUBLE NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bid_auction  FOREIGN KEY (auction_id)  REFERENCES auctions (id),
    CONSTRAINT fk_bid_investor FOREIGN KEY (investor_id) REFERENCES users (id)
);

-- Auction winners table
CREATE TABLE IF NOT EXISTS auction_winners (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id  BIGINT NOT NULL UNIQUE,
    winner_id   BIGINT NOT NULL,
    winning_bid DOUBLE NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_winner_auction   FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_winner_investor  FOREIGN KEY (winner_id)  REFERENCES users (id)
);
