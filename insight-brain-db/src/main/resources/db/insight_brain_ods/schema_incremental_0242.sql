-- Since 1.127
ALTER TABLE source_control ADD COLUMN normalized_repository_url varchar(2048);

UPDATE source_control SET normalized_repository_url = repository_url;

CREATE INDEX source_control_normalized_repository_url_idx ON source_control(normalized_repository_url);
