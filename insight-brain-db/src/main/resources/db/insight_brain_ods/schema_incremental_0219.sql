-- Since 1.114
UPDATE source_control SET repository_url=LOWER(repository_url);

ALTER TABLE source_control_pull_request RENAME COLUMN repository_url_lowercase TO repository_url;
