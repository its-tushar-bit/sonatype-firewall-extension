-- Since 1.157
CREATE TABLE saml_group(
  saml_group_id varchar(50) NOT NULL,
  name varchar(2048) NOT NULL,
  CONSTRAINT saml_group_pk PRIMARY KEY (saml_group_id),
  CONSTRAINT saml_group_name_uk UNIQUE (name)
);

CREATE TABLE saml_user_group (
  saml_user_group_id varchar(50) NOT NULL,
  saml_user_id varchar(50) NOT NULL,
  saml_group_id varchar(50) NOT NULL,
  CONSTRAINT saml_user_group_pk PRIMARY KEY (saml_user_group_id),
  CONSTRAINT saml_user_group_uk UNIQUE (saml_user_id, saml_group_id),
  CONSTRAINT saml_user_group_user_fk FOREIGN KEY (saml_user_id) REFERENCES saml_user(saml_user_id),
  CONSTRAINT saml_user_group_group_fk FOREIGN KEY (saml_group_id) REFERENCES saml_group(saml_group_id)
);
