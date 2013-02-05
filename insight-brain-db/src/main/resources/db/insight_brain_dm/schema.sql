-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE license_category (
  license_category_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  severity smallint(2) NOT NULL,
  CONSTRAINT license_category_pk PRIMARY KEY (license_category_id),
  UNIQUE KEY license_category_name_uk (name)
);

CREATE TABLE license (
  license_id varchar(50) NOT NULL,
  shortDisplayName varchar(50) NOT NULL,
  longDisplayName varchar(200) default NULL,
  description varchar(400) default NULL,
  licenseUrl varchar(100) default NULL,
  license_category_id varchar(50) NULL,
  CONSTRAINT license_pk PRIMARY KEY (license_id),
  UNIQUE KEY license_shortDisplayName_uk (shortDisplayName),
  CONSTRAINT license_license_category_fk FOREIGN KEY (license_category_id) REFERENCES license_category(license_category_id)
);

CREATE TABLE multi_license (
  multi_license_id varchar(50) NOT NULL,
  shortDisplayName varchar(50) NOT NULL,
  longDisplayName varchar(200) default NULL,
  description varchar(400) default NULL,
  licenseUrl varchar(100) default NULL,
  CONSTRAINT multi_license_pk PRIMARY KEY (multi_license_id),
  UNIQUE KEY multi_license_shortDisplayName_uk (shortDisplayName)
);

CREATE TABLE multi_license_license (
  multi_license_id varchar(50) NOT NULL,
  license_id varchar(50) NOT NULL,
  CONSTRAINT multi_license_license_pk PRIMARY KEY (multi_license_id, license_id),
  CONSTRAINT multi_license_license_multi_fk FOREIGN KEY (multi_license_id) REFERENCES multi_license(multi_license_id),
  CONSTRAINT multi_license_license_license_fk FOREIGN KEY (license_id) REFERENCES license(license_id)
);
