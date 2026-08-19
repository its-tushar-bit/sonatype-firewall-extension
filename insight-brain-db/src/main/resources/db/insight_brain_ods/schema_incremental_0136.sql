-- Since 1.70
ALTER TABLE source_control ADD COLUMN provider varchar(20) NULL;

UPDATE source_control SET provider = 'GITHUB';

ALTER TABLE source_control ALTER COLUMN provider SET NOT NULL;
