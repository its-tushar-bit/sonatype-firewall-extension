-- Since 1.75.0
CREATE TABLE user_token (
  user_token_id varchar(50) NOT NULL,
  username varchar(200) NOT NULL,
  user_code varchar(128) NOT NULL,
  pass_code varchar(128) NOT NULL,
  create_time timestamp NOT NULL,
  CONSTRAINT user_token_pk PRIMARY KEY (user_token_id),
  CONSTRAINT user_token_uk UNIQUE (username)
);
