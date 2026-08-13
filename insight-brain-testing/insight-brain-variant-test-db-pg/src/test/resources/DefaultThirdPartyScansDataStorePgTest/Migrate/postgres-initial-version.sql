--
-- DO NOT CHANGE THIS FILE!
--
-- Given this as baseline, new migration scripts must be able to upgrade the PostgreSQL database.
--

CREATE SCHEMA insight_brain_third_party_scans;
SET SCHEMA 'insight_brain_third_party_scans';

-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE scanned_file (
  scanned_file_id VARCHAR(50) NOT NULL,
  hash VARCHAR(20) NOT NULL,
  filename VARCHAR(1000) NOT NULL,
  image VARCHAR(1000) NOT NULL,
  create_time TIMESTAMP NOT NULL,
  CONSTRAINT scanned_file_pk PRIMARY KEY (scanned_file_id),
  CONSTRAINT scanned_file_uk UNIQUE (hash)
);
CREATE INDEX scanned_file_image ON scanned_file (image);

CREATE TABLE scanned_file_mapping (
  scanned_file_mapping_id VARCHAR(50) NOT NULL,
  scanned_file_id VARCHAR(50) NOT NULL,
  scan_id VARCHAR(50) NOT NULL,
  CONSTRAINT scanned_file_mapping_pk PRIMARY KEY (scanned_file_mapping_id),
  CONSTRAINT scanned_file_mapping_uk UNIQUE (scanned_file_id, scan_id),
  CONSTRAINT scanned_file_mapping_fk FOREIGN KEY (scanned_file_id) REFERENCES scanned_file (scanned_file_id)
);
CREATE INDEX scanned_file_mapping_scan_id ON scanned_file_mapping (scan_id);

CREATE TABLE coordinate_file (
  coordinate_file_id VARCHAR(50) NOT NULL,
  hash VARCHAR(20) NOT NULL,
  source VARCHAR(20) NOT NULL,
  format VARCHAR(10) NOT NULL,
  name VARCHAR(300) NOT NULL,
  version VARCHAR(200) NOT NULL,
  scanned_file_id VARCHAR(50) NOT NULL,
  CONSTRAINT coordinate_file_pk PRIMARY KEY (coordinate_file_id),
  CONSTRAINT coordinate_file_uk UNIQUE (source, format, name, version, scanned_file_id),
  CONSTRAINT coordinate_file_fk FOREIGN KEY (scanned_file_id) REFERENCES scanned_file (scanned_file_id)
);

CREATE TABLE coordinate_security (
  coordinate_security_id VARCHAR(50) NOT NULL,
  coordinate_file_id VARCHAR(50) NOT NULL,
  ref_id VARCHAR(20) NOT NULL,
  description TEXT,
  link VARCHAR(200),
  severity FLOAT NOT NULL,
  fixed_by VARCHAR(200),
  CONSTRAINT coordinate_security_pk PRIMARY KEY (coordinate_security_id),
  CONSTRAINT coordinate_security_uk UNIQUE (coordinate_file_id, ref_id),
  CONSTRAINT coordinate_security_fk FOREIGN KEY (coordinate_file_id) REFERENCES coordinate_file(coordinate_file_id)
);

CREATE TABLE schema_version (
  schema_version int NOT NULL
);
INSERT INTO schema_version (schema_version) VALUES (1);
