-- For tests only
CREATE TABLE IF NOT EXISTS test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

-- In SBOM Manager: represents an upload
-- In Lifecycle: represents an SBOM found within a scan. Note that in Lifecycle this record is deleted after report
-- processing completes.
CREATE TABLE third_party_file (
  third_party_file_id VARCHAR(50) NOT NULL,

  -- In SBOM Manager: the user-provided filename for the SBOM file. Not the name where it is stored on the server.
  -- In Lifecycle: the name of the file as it was within the scan.xml
  filename VARCHAR(1000) NULL,
  create_time TIMESTAMP NOT NULL,
  CONSTRAINT third_party_file_pk PRIMARY KEY (third_party_file_id)
);

-- In SBOM Manager: represents the scan of an upload
-- In Lifecycle: represents the scan within which an SBOM was found. However for reasons unknown, a separate
-- third_party_scan row is created for each SBOM file within the scan, rather than one row per scan, associated
-- with many third_party_file rows. Note that in Lifecycle this record is deleted after report processing completes.
CREATE TABLE third_party_scan (
  third_party_scan_id VARCHAR(50) NOT NULL,
  third_party_file_id VARCHAR(50) NOT NULL,
  scan_request_id VARCHAR(50) NOT NULL,
  scan_id VARCHAR(50) NULL,
  create_time TIMESTAMP NOT NULL,
  filtered_scan_file VARCHAR(1000) NULL,
  previous_scan_id VARCHAR(50) NULL,
  CONSTRAINT third_party_scan_pk PRIMARY KEY (third_party_scan_id),
  CONSTRAINT third_party_scan_uk UNIQUE (third_party_file_id, scan_request_id),
  CONSTRAINT third_party_scan_fk FOREIGN KEY (third_party_file_id) REFERENCES third_party_file (third_party_file_id)
);
CREATE INDEX third_party_scan_scan_id ON third_party_scan (scan_id);
CREATE INDEX third_party_scan_scan_request_id ON third_party_scan (scan_request_id);

CREATE TABLE file_coordinate (
  file_coordinate_id VARCHAR(50) NOT NULL,
  hash VARCHAR(20) NOT NULL,
  source VARCHAR(100) NOT NULL,
  format VARCHAR(50) NOT NULL,
  name VARCHAR(300) NOT NULL,
  version VARCHAR(200) NOT NULL,
  third_party_file_id VARCHAR(50) NOT NULL,
  package_url VARCHAR(1000)  NULL,
  cpe VARCHAR(1000) NULL,
  swid TEXT NULL,
  dependency_type        VARCHAR(2)   NULL,
  identification_sources VARCHAR(100) NULL,
  match_state_id VARCHAR(20) NULL,
  occurrences TEXT NULL,
  filenames TEXT NULL,
  display_name VARCHAR(1000) NULL,
  component_ref VARCHAR(50) NULL,
  CONSTRAINT file_coordinate_pk PRIMARY KEY (file_coordinate_id),
  CONSTRAINT file_coordinate_fk FOREIGN KEY (third_party_file_id) REFERENCES third_party_file (third_party_file_id)
);
CREATE INDEX file_coordinate_third_party_file_id_idx ON file_coordinate (third_party_file_id);
CREATE INDEX file_coordinate_third_party_file_id_component_ref_idx ON file_coordinate (third_party_file_id, component_ref);
CREATE INDEX file_coordinate_third_party_file_id_hash_idx ON file_coordinate (third_party_file_id, hash);

CREATE TABLE coordinate_security (
  coordinate_security_id VARCHAR(50) NOT NULL,
  file_coordinate_id VARCHAR(50) NOT NULL,
  ref_id VARCHAR(255) NOT NULL,
  description TEXT,
  link VARCHAR(200),
  severity FLOAT NOT NULL,
  fixed_by VARCHAR(200),
  vulnerability_source VARCHAR(50),
  severity_description VARCHAR(15),
  attack_vector VARCHAR(255),
  rating_method VARCHAR(10),
  cwes TEXT,
  recommendations TEXT,
  advisories TEXT,
  identification_sources VARCHAR(100) NULL,
  sbom_metadata_id VARCHAR(50),
  research_type VARCHAR(50),
  detection_type VARCHAR(50),
  vuln_ids TEXT,
  CONSTRAINT coordinate_security_pk PRIMARY KEY (coordinate_security_id),
  CONSTRAINT coordinate_security_uk UNIQUE (file_coordinate_id, ref_id),
  CONSTRAINT coordinate_security_fk FOREIGN KEY (file_coordinate_id) REFERENCES file_coordinate(file_coordinate_id)
);
CREATE INDEX coordinate_security_sbom_metadata_id_idx ON coordinate_security (sbom_metadata_id);

CREATE TABLE coordinate_license (
  coordinate_license_id VARCHAR(50) NOT NULL,
  file_coordinate_id VARCHAR(50) NOT NULL,
  license_id VARCHAR(1000) NOT NULL,
  name TEXT NULL,
  url VARCHAR(200) NULL,
  identification_sources VARCHAR(100) NULL,
  CONSTRAINT license_coordinate_pk PRIMARY KEY (coordinate_license_id),
  CONSTRAINT license_coordinate_uk UNIQUE (license_id, file_coordinate_id),
  CONSTRAINT license_coordinate_fk FOREIGN KEY (file_coordinate_id) REFERENCES file_coordinate(file_coordinate_id)
);
CREATE INDEX coordinate_license_file_coordinate_id ON coordinate_license (file_coordinate_id);

CREATE TABLE third_party_vulnerability (
  third_party_vulnerability_id VARCHAR(50) NOT NULL,
  ref_id VARCHAR(255) NOT NULL,
  description TEXT,
  link VARCHAR(200),
  severity FLOAT NOT NULL,
  fixed_by VARCHAR(200),
  vulnerability_source VARCHAR(50),
  severity_description VARCHAR(15),
  attack_vector VARCHAR(255),
  rating_method VARCHAR(10),
  cwes TEXT,
  recommendations TEXT,
  advisories TEXT,
  update_time TIMESTAMP, -- when was this vulnerability information last updated
  CONSTRAINT third_party_vulnerability_pk PRIMARY KEY (third_party_vulnerability_id),
  CONSTRAINT third_party_vulnerability_refid_uk UNIQUE (ref_id)
);

CREATE TABLE vulnerability_exploitability (
  vulnerability_exploitability_id VARCHAR(50) NOT NULL,
  coordinate_security_id VARCHAR(50) NOT NULL,
  ref_id VARCHAR(255) NOT NULL,
  state VARCHAR(50),
  justification VARCHAR(200),
  response VARCHAR(50),
  detail TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  last_updated_by VARCHAR(255),
  CONSTRAINT vulnerability_exploitability_pk PRIMARY KEY (vulnerability_exploitability_id),
  CONSTRAINT vulnerability_exploitability_coordinate_security_fk FOREIGN KEY (coordinate_security_id)
      REFERENCES coordinate_security (coordinate_security_id)
);

CREATE INDEX vulnerability_exploitability_coord_security_id_ref_id_idx ON vulnerability_exploitability (coordinate_security_id, ref_id);

-- In SBOM Manager: in conjunction with third_party_file, represents an upload
-- In Lifecycle: unused
CREATE TABLE sbom_metadata (
    sbom_metadata_id VARCHAR(50) NOT NULL,
    third_party_file_id VARCHAR(50) NOT NULL,
    application_id VARCHAR(50) NOT NULL,

    -- The basename of the file where the SBOM contents are stored on the server. Not the user-provided name of the SBOM
    -- file. Null if the SBOM contents are not (yet) stored, which can be the case with binary files scanned as SBOMs
    -- when they are still in the UPLOADED status. Once an SBOM is generated for a binary, the basename of that
    -- generated SBOM file is stored here. The binary file itself is not stored long-term at all.
    file_name VARCHAR(200),
    serial_number VARCHAR(2000),
    spec VARCHAR(50) NOT NULL,
    spec_format VARCHAR(50) NOT NULL,
    spec_version VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    metadata_json TEXT,
    sbom_version VARCHAR(1100) NOT NULL,
    scan_type VARCHAR(20) NOT NULL,
    validation_skipped BOOLEAN,
    is_valid BOOLEAN,

    -- The user-provided filename for the SBOM file. Not the name where it is stored on the server.
    -- Seemingly redundant with third_party_file.filename; more investigation needed.
    original_binary_file_name TEXT,
    extended_profile_elements TEXT,
    root_component_ref VARCHAR(40),
    CONSTRAINT sbom_metadata_pk PRIMARY KEY (sbom_metadata_id),
    CONSTRAINT sbom_metadata_third_party_file_fk FOREIGN KEY (third_party_file_id)
       REFERENCES third_party_file (third_party_file_id),
    CONSTRAINT sbom_metadata_sbom_version_uk UNIQUE (application_id, sbom_version)
);
CREATE INDEX application_id_idx ON sbom_metadata(application_id);
CREATE INDEX sbom_metadata_status_idx ON sbom_metadata (status);
CREATE INDEX sbom_metadata_third_party_file_id_idx ON sbom_metadata(third_party_file_id);
CREATE INDEX sbom_metadata_status_app_id_created_at_id_idx ON sbom_metadata(status, application_id, created_at, sbom_metadata_id);

CREATE TABLE IF NOT EXISTS schema_version (
  data_store_id varchar(32) NOT NULL,
  schema_version int NOT NULL
);
INSERT INTO schema_version (data_store_id , schema_version) VALUES ('insight_brain_third_party_scans', -1);

CREATE TABLE third_party_unknown_component
(
    unknown_component_id VARCHAR(50)   NOT NULL,
    filename             VARCHAR(1000) NOT NULL,
    hash                 VARCHAR(20)   NOT NULL,
    dependency_type      VARCHAR(2),
    third_party_file_id  VARCHAR(50)   NOT NULL,
    CONSTRAINT unknown_component_pk PRIMARY KEY (unknown_component_id),
    CONSTRAINT unknown_component_uk UNIQUE (unknown_component_id),
    CONSTRAINT unknown_component_third_party_file_id_fk FOREIGN KEY (third_party_file_id)
        REFERENCES third_party_file (third_party_file_id)
);

CREATE INDEX unknown_component_third_party_file_id_idx
    ON third_party_unknown_component (third_party_file_id);
