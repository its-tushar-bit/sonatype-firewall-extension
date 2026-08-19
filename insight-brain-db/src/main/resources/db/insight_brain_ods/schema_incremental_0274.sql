-- Since 1.152
CREATE TABLE vulnerability_custom_detail
(
  vulnerability_custom_detail_id VARCHAR(50) NOT NULL,
  owner_id VARCHAR(50) NOT NULL,
  refid VARCHAR(20) NOT NULL,
  component_id_format VARCHAR(10),
  component_id_coordinates_json VARCHAR(1000),
  application_tag_id VARCHAR(50),
  severity FLOAT DEFAULT NULL,
  cvss_vector_string VARCHAR(50) DEFAULT NULL,
  cwe VARCHAR(50) DEFAULT NULL,
  remediation VARCHAR(3000) DEFAULT NULL,
  comment VARCHAR(1000) DEFAULT NULL,
  last_updated_by_username VARCHAR(256) NOT NULL,
  last_updated_at TIMESTAMP NOT NULL,
  CONSTRAINT vulnerability_custom_detail_pk PRIMARY KEY (vulnerability_custom_detail_id),
  CONSTRAINT vulnerability_custom_detail_fk FOREIGN KEY (application_tag_id) REFERENCES tag (tag_id),
  CONSTRAINT vulnerability_custom_detail_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json)
);
CREATE INDEX vulnerability_custom_detail_owner_refid_component_idx ON vulnerability_custom_detail(owner_id, refid, component_id_format, component_id_coordinates_json);
CREATE INDEX vulnerability_custom_detail_owner_refid_idx ON vulnerability_custom_detail(owner_id, refid);

