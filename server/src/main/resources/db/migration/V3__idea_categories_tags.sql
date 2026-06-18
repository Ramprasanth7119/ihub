-- Phase 2: Idea workflow, categories, tags

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tags (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS idea_tags (
    idea_id     BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (idea_id, tag_id),
    CONSTRAINT fk_idea_tags_idea FOREIGN KEY (idea_id) REFERENCES ideas(id) ON DELETE CASCADE,
    CONSTRAINT fk_idea_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Run on existing databases (spring.sql.init does not apply ALTER automatically)
ALTER TABLE ideas ADD COLUMN IF NOT EXISTS max_budget DECIMAL(12,2) NULL AFTER base_price;
ALTER TABLE ideas MODIFY status VARCHAR(20) DEFAULT 'DRAFT';
UPDATE ideas SET status = 'PUBLISHED' WHERE status IN ('OPEN', 'open');
UPDATE ideas SET max_budget = base_price WHERE max_budget IS NULL;
