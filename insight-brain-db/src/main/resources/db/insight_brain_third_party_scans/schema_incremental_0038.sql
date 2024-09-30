-- since 1.183
-- SaaS compatible

ALTER TABLE file_coordinate
    ADD COLUMN website VARCHAR(1000) NULL;

ALTER TABLE file_coordinate
    ADD COLUMN category_ids TEXT NULL;