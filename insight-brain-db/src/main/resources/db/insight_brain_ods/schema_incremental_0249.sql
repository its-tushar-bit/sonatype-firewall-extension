-- Since 1.133
CREATE TABLE saml_user (
  saml_user_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL,
  first_name varchar(100) NULL,
  last_name varchar(100) NULL,
  email varchar(255) NULL,
  groups varchar(8192) NULL,
  CONSTRAINT saml_user_pk PRIMARY KEY (saml_user_id),
  CONSTRAINT saml_user_username_uk UNIQUE (username)
);
