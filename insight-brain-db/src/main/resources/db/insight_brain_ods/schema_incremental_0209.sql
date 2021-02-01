-- Since 1.105
CREATE TABLE user_filter (
  user_filter_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL, -- The internal name of the User
  username_lowercase varchar(60) NOT NULL,
  realm_id varchar(50) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  filter_json text NOT NULL, -- The filter stored in json format
  based_on_filter_name varchar(60),
  filter_type varchar(100) NOT NULL,
  CONSTRAINT user_filter_pk PRIMARY KEY (user_filter_id),
  CONSTRAINT user_filter_uk UNIQUE (username_lowercase, realm_id, name_lowercase_no_whitespace, filter_type)
);
