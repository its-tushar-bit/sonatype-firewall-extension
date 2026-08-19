-- Since 1.76.0
ALTER TABLE user_token ADD COLUMN username_lowercase varchar(200) NOT NULL;
ALTER TABLE user_token ADD COLUMN internal_user boolean NOT NULL;

ALTER TABLE user_token DROP CONSTRAINT user_token_uk;
ALTER TABLE user_token ADD CONSTRAINT user_token_uk UNIQUE (username_lowercase);
