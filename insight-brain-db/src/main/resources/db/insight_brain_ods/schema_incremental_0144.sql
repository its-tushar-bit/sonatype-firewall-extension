-- Since 1.76.0
ALTER TABLE user_token ADD CONSTRAINT user_token_user_code_uk UNIQUE (user_code);
