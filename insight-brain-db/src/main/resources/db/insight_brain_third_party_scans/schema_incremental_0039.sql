-- since 1.183
-- SaaS compatible

ALTER TABLE file_coordinate
    ADD COLUMN occurrences TEXT NULL;
