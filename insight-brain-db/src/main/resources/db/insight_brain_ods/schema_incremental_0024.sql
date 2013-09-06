SET SCHEMA insight_brain_ods;

CREATE TABLE user (
  user_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL,
  username_lowercase varchar(60) NOT NULL,
  password_hash varchar(128) NULL,
  first_name varchar(100) NOT NULL,
  last_name varchar(100) NOT NULL,
  email varchar(255) NULL,
  CONSTRAINT user_pk PRIMARY KEY (user_id),
  CONSTRAINT user_username_uk UNIQUE KEY (username_lowercase)
);
INSERT INTO user (user_id, username, username_lowercase, first_name, last_name ) VALUES ('ADMIN', 'admin', 'admin', 'Admin', 'BuiltIn');
