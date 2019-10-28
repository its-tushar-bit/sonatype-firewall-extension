-- Since 1.76.0

DROP TABLE user_token;

CREATE TABLE user_token (
  user_token_id varchar(50) NOT NULL,
  username varchar(200) NOT NULL,
  user_code varchar(128) NOT NULL,
  pass_code varchar(128) NOT NULL,
  realm_id varchar(50) NOT NULL,
  create_time timestamp NOT NULL,
  CONSTRAINT user_token_pk PRIMARY KEY (user_token_id),
  CONSTRAINT user_token_uk UNIQUE (username, realm_id),
  CONSTRAINT user_token_user_code_uk UNIQUE (user_code)
);
