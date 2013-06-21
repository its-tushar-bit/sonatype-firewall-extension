-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

-- The public_id column is what we expose as AppID to the user
CREATE TABLE application (
  application_id varchar(50) NOT NULL,
  public_id varchar(60) NOT NULL,
  public_id_lowercase varchar(60) NOT NULL,
  name varchar(60) NOT NULL,
  name_lowercase_no_whitespace varchar(60) NOT NULL,
  CONSTRAINT application_pk PRIMARY KEY (application_id),
  CONSTRAINT application_uk UNIQUE KEY (public_id_lowercase),
  CONSTRAINT application_name_uk UNIQUE KEY (name_lowercase_no_whitespace)
);

CREATE TABLE label (
  label_id varchar(50) NOT NULL,
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
  label_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  CONSTRAINT component_label_pk PRIMARY KEY (component_label_id),
  CONSTRAINT component_label_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
  CONSTRAINT component_label_label_fk FOREIGN KEY (label_id) REFERENCES label(label_id),
  CONSTRAINT component_label_uk UNIQUE KEY (application_id, hash, label_id)
);

CREATE TABLE license_threat_group (
  license_threat_group_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  threat_level smallint(2) NOT NULL,
  CONSTRAINT license_threat_group_pk PRIMARY KEY (license_threat_group_id),
  CONSTRAINT license_threat_group_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
  CONSTRAINT license_threat_group_uk UNIQUE KEY (application_id, name)
);

CREATE TABLE license_threat_group_license (
  license_threat_group_license_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  license_threat_group_id varchar(50) NOT NULL,
  license_id varchar(50) NOT NULL,
  CONSTRAINT license_threat_group_license_pk PRIMARY KEY (license_threat_group_license_id),
  CONSTRAINT license_threat_group_license_application_fk FOREIGN KEY (application_id) REFERENCES application(application_id),
  CONSTRAINT license_threat_group_license_group_fk FOREIGN KEY (license_threat_group_id) REFERENCES license_threat_group(license_threat_group_id),
  CONSTRAINT license_threat_group_license_uk UNIQUE KEY (application_id, license_id)
);

CREATE TABLE hash_gav (
  hash_gav_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  group_id varchar(100) NOT NULL,
  artifact_id varchar(100) NOT NULL,
  version varchar(100) NOT NULL,
  extension varchar(50),
  classifier varchar(50),
  comment varchar(1000) NULL,
  CONSTRAINT hash_gav_id PRIMARY KEY (hash_gav_id),
  CONSTRAINT hash_gav_hash_uk UNIQUE KEY (hash),
  CONSTRAINT hash_gav_gavec_uk UNIQUE KEY (group_id, artifact_id, version, extension, classifier)
);
