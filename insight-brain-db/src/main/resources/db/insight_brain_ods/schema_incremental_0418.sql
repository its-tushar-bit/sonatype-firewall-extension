-- SaaS Compatible

ALTER TABLE user_token ADD COLUMN IF NOT EXISTS last_access_time timestamp NULL;
