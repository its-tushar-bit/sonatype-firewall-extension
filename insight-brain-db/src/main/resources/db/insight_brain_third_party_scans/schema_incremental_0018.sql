-- since 1.171
-- SaaS Compatible

ALTER TABLE file_coordinate ADD COLUMN cpe varchar(255);
ALTER TABLE file_coordinate ADD COLUMN swid text;
