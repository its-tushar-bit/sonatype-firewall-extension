-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

-- The public_id column is what we expose as AppID to the user
CREATE TABLE application (
  application_id varchar(50) NOT NULL,
  public_id varchar(50) NOT NULL,
  CONSTRAINT application_pk PRIMARY KEY (application_id)
);

CREATE TABLE label (
  label_id  varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  label varchar(50) NOT NULL,
  label_lowercase varchar(50) NOT NULL,
  color varchar(20) NULL,
  CONSTRAINT label_pk PRIMARY KEY (label_id),
  CONSTRAINT label_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
  CONSTRAINT label_uk UNIQUE KEY (application_id, label_lowercase)
);

CREATE TABLE component_label (
  component_label_id  varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  label varchar(50) NOT NULL,
  label_lowercase varchar(50) NOT NULL,
  active bool DEFAULT true NOT NULL,
  create_user_id varchar(254) NOT NULL,
  create_time datetime NOT NULL,
  delete_user_id varchar(254),
  delete_time datetime NOT NULL,
  CONSTRAINT component_label_pk PRIMARY KEY (component_label_id),
  CONSTRAINT component_label_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
  CONSTRAINT component_label_uk UNIQUE KEY (application_id, hash, label_lowercase, delete_time)
);
