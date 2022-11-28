-- 
-- DO NOT CHANGE THIS FILE!
-- 
-- This is a dump of the database schema at the time PostgreSQL support was added.
-- Given this as baseline, new migration scripts must be able to upgrade the PostgreSQL database.
--

CREATE SCHEMA insight_brain_dm;
SET SCHEMA 'insight_brain_dm';

-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE license (
  license_id varchar(1000) NOT NULL,
  shortDisplayName varchar(1000) NOT NULL,
  longDisplayName varchar(1000) default NULL,
  CONSTRAINT license_pk PRIMARY KEY (license_id),
  CONSTRAINT license_shortDisplayName_uk UNIQUE (shortDisplayName)
);

CREATE TABLE multi_license (
  multi_license_id varchar(1000) NOT NULL,
  shortDisplayName varchar(1000) NOT NULL,
  longDisplayName varchar(1000) default NULL,
  CONSTRAINT multi_license_pk PRIMARY KEY (multi_license_id),
  CONSTRAINT multi_license_shortDisplayName_uk UNIQUE (shortDisplayName)
);

CREATE TABLE multi_license_license (
  multi_license_id varchar(1000) NOT NULL,
  license_id varchar(1000) NOT NULL,
  CONSTRAINT multi_license_license_pk PRIMARY KEY (multi_license_id, license_id),
  CONSTRAINT multi_license_license_multi_fk FOREIGN KEY (multi_license_id) REFERENCES multi_license(multi_license_id),
  CONSTRAINT multi_license_license_license_fk FOREIGN KEY (license_id) REFERENCES license(license_id)
);

CREATE TABLE schema_version (
  schema_version int NOT NULL
);
INSERT INTO schema_version (schema_version) VALUES (8);
