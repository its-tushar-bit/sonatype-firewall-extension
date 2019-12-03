-- Since 1.72.0
-- drop all tables
drop table coordinate_security;
drop table coordinate_file;
drop table scanned_file_mapping;
drop table scanned_file;

-- recreate refactored tables
CREATE TABLE third_party_file (
  third_party_file_id VARCHAR(50) NOT NULL,
  hash VARCHAR(20) NOT NULL,
  filename VARCHAR(1000) NOT NULL,
  image VARCHAR(1000) NULL,
  create_time TIMESTAMP NOT NULL,
  CONSTRAINT third_party_file_pk PRIMARY KEY (third_party_file_id),
  CONSTRAINT third_party_file_uk UNIQUE (hash)
);

CREATE TABLE third_party_scan (
  third_party_scan_id VARCHAR(50) NOT NULL,
  third_party_file_id VARCHAR(50) NOT NULL,
  scan_id VARCHAR(50) NOT NULL,
  create_time TIMESTAMP NOT NULL,
  CONSTRAINT third_party_scan_pk PRIMARY KEY (third_party_scan_id),
  CONSTRAINT third_party_scan_uk UNIQUE (third_party_file_id, scan_id),
  CONSTRAINT third_party_scan_fk FOREIGN KEY (third_party_file_id) REFERENCES third_party_file (third_party_file_id)
);
CREATE INDEX third_party_scan_scan_id ON third_party_scan (scan_id);

CREATE TABLE file_coordinate (
  file_coordinate_id VARCHAR(50) NOT NULL,
  hash VARCHAR(20) NOT NULL,
  source VARCHAR(20) NOT NULL,
  format VARCHAR(10) NOT NULL,
  name VARCHAR(300) NOT NULL,
  version VARCHAR(200) NOT NULL,
  third_party_file_id VARCHAR(50) NOT NULL,
  CONSTRAINT file_coordinate_pk PRIMARY KEY (file_coordinate_id),
  CONSTRAINT file_coordinate_uk UNIQUE (source, format, name, version, third_party_file_id),
  CONSTRAINT file_coordinate_fk FOREIGN KEY (third_party_file_id) REFERENCES third_party_file (third_party_file_id)
);

CREATE TABLE coordinate_security (
  coordinate_security_id VARCHAR(50) NOT NULL,
  file_coordinate_id VARCHAR(50) NOT NULL,
  ref_id VARCHAR(20) NOT NULL,
  description TEXT,
  link VARCHAR(200),
  severity FLOAT NOT NULL,
  fixed_by VARCHAR(200),
  CONSTRAINT coordinate_security_pk PRIMARY KEY (coordinate_security_id),
  CONSTRAINT coordinate_security_uk UNIQUE (file_coordinate_id, ref_id),
  CONSTRAINT coordinate_security_fk FOREIGN KEY (file_coordinate_id) REFERENCES file_coordinate(file_coordinate_id)
);
