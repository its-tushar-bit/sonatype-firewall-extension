-- since 1.177
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS oauth2_user
(
  oauth2_user_id varchar(255) NOT NULL,
  username varchar(255) NOT NULL,
  first_name varchar(255) NULL,
  last_name varchar(255) NULL,
  email varchar(255) NULL,
  groups_json text NULL,
  CONSTRAINT oauth2_user_pk PRIMARY KEY (oauth2_user_id),
  CONSTRAINT oauth2_user_username_uk UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS oauth2_group
(
  oauth2_group_id varchar(255) NOT NULL,
  name text NOT NULL,
  CONSTRAINT oauth2_group_pk PRIMARY KEY (oauth2_group_id)
);

CREATE TABLE IF NOT EXISTS oauth2_user_group
(
  oauth2_user_group_id varchar(255) NOT NULL,
  oauth2_user_id varchar(255) NOT NULL,
  oauth2_group_id varchar(255) NOT NULL,
  CONSTRAINT oauth2_user_group_pk PRIMARY KEY (oauth2_user_group_id),
  CONSTRAINT oauth2_user_group_uk UNIQUE (oauth2_user_id, oauth2_group_id),
  CONSTRAINT oauth2_user_group_user_fk FOREIGN KEY (oauth2_user_id) REFERENCES oauth2_user(oauth2_user_id),
  CONSTRAINT oauth2_user_group_group_fk FOREIGN KEY (oauth2_group_id) REFERENCES oauth2_group(oauth2_group_id)
);
