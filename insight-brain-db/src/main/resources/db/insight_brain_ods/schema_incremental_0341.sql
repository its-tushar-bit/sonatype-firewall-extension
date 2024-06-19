-- since 1.178
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS development_prioritization (
  development_prioritization_id varchar(50) NOT NULL,
  scan_id varchar(50) NOT NULL,
  created_at timestamp DEFAULT now() NOT NULL,
  updated_at timestamp DEFAULT now() NOT NULL,
  CONSTRAINT development_prioritization_pk PRIMARY KEY (development_prioritization_id),
  CONSTRAINT development_prioritization_scan_id_uk UNIQUE (scan_id)
  );

CREATE TABLE IF NOT EXISTS development_prioritization_component_info (
  development_prioritization_component_info_id varchar(50) NOT NULL,
  development_prioritization_id varchar(50) NOT NULL,
  scan_id varchar(50) NOT NULL,
  component_hash varchar(20) NOT NULL,
  remediation_type varchar(50), -- maps to enum ApiVersionChangeOptionType
  remediation_version varchar(100),
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  CONSTRAINT development_prioritization_component_info_pk PRIMARY KEY (development_prioritization_component_info_id),
  CONSTRAINT development_prioritization_component_scan_id_hash_uk UNIQUE (scan_id, component_hash),
  CONSTRAINT development_prioritization_component_info_parent_fk FOREIGN KEY (development_prioritization_id) REFERENCES development_prioritization(development_prioritization_id)
  );

CREATE INDEX development_prioritization_component_scan_id_hash_idx ON development_prioritization_component_info(scan_id, component_hash);
