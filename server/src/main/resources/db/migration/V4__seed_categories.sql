-- Reference categories. INSERT IGNORE keeps re-running harmless and lets an
-- operator add more rows without this migration fighting them.

INSERT IGNORE INTO categories (name, slug) VALUES
    ('Technology', 'technology'),
    ('Healthcare', 'healthcare'),
    ('Finance', 'finance'),
    ('Education', 'education'),
    ('Sustainability', 'sustainability'),
    ('Consumer', 'consumer');
