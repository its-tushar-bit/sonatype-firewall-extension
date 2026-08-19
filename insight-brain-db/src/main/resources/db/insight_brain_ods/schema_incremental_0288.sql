-- Since 1.160
DELETE FROM vulnerability_custom_detail;

CREATE TABLE vulnerability_custom_remediation
(
    vulnerability_custom_remediation_id VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50) NOT NULL,
    refid VARCHAR(20) NOT NULL,
    component_id_format VARCHAR(10),
    component_id_coordinates_json VARCHAR(1000),
    remediation VARCHAR(3000) NOT NULL,
    last_updated_by_username VARCHAR(256) NOT NULL,
    last_updated_at TIMESTAMP NOT NULL,
    CONSTRAINT vulnerability_custom_remediation_pk PRIMARY KEY (vulnerability_custom_remediation_id),
    CONSTRAINT vulnerability_custom_remediation_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json)
);

CREATE TABLE vulnerability_custom_remediation_tag
(
    vulnerability_custom_remediation_tag_id VARCHAR(50) NOT NULL,
    vulnerability_custom_remediation_id VARCHAR(50) NOT NULL,
    tag_id VARCHAR(50) NOT NULL,
    CONSTRAINT vulnerability_custom_remediation_tag_id PRIMARY KEY (vulnerability_custom_remediation_tag_id),
    CONSTRAINT vulnerability_custom_remediation_tag_remediation_fk FOREIGN KEY (vulnerability_custom_remediation_id) REFERENCES vulnerability_custom_remediation (vulnerability_custom_remediation_id),
    CONSTRAINT vulnerability_custom_remediation_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
    CONSTRAINT vulnerability_custom_remediation_tag_uk UNIQUE (vulnerability_custom_remediation_id, tag_id)
);

CREATE TABLE vulnerability_custom_cwe
(
    vulnerability_custom_cwe_id VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50) NOT NULL,
    refid VARCHAR(20) NOT NULL,
    component_id_format VARCHAR(10),
    component_id_coordinates_json VARCHAR(1000),
    cwe VARCHAR(50) NOT NULL,
    last_updated_by_username VARCHAR(256) NOT NULL,
    last_updated_at TIMESTAMP NOT NULL,
    CONSTRAINT vulnerability_custom_cwe_pk PRIMARY KEY (vulnerability_custom_cwe_id),
    CONSTRAINT vulnerability_custom_cwe_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json)
);

CREATE TABLE vulnerability_custom_cwe_tag
(
    vulnerability_custom_cwe_tag_id VARCHAR(50) NOT NULL,
    vulnerability_custom_cwe_id VARCHAR(50) NOT NULL,
    tag_id VARCHAR(50) NOT NULL,
    CONSTRAINT vulnerability_custom_cwe_tag_id PRIMARY KEY (vulnerability_custom_cwe_tag_id),
    CONSTRAINT vulnerability_custom_cwe_tag_cwe_fk FOREIGN KEY (vulnerability_custom_cwe_id) REFERENCES vulnerability_custom_cwe (vulnerability_custom_cwe_id),
    CONSTRAINT vulnerability_custom_cwe_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
    CONSTRAINT vulnerability_custom_cwe_tag_uk UNIQUE (vulnerability_custom_cwe_id, tag_id)
);

CREATE TABLE vulnerability_custom_cvss_vector
(
    vulnerability_custom_cvss_vector_id VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50) NOT NULL,
    refid VARCHAR(20) NOT NULL,
    component_id_format VARCHAR(10),
    component_id_coordinates_json VARCHAR(1000),
    vector VARCHAR(200) NOT NULL,
    last_updated_by_username VARCHAR(256) NOT NULL,
    last_updated_at TIMESTAMP NOT NULL,
    CONSTRAINT vulnerability_custom_cvss_vector_pk PRIMARY KEY (vulnerability_custom_cvss_vector_id),
    CONSTRAINT vulnerability_custom_cvss_vector_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json)
);

CREATE TABLE vulnerability_custom_cvss_vector_tag
(
    vulnerability_custom_cvss_vector_tag_id VARCHAR(50) NOT NULL,
    vulnerability_custom_cvss_vector_id VARCHAR(50) NOT NULL,
    tag_id VARCHAR(50) NOT NULL,
    CONSTRAINT vulnerability_custom_cvss_vector_tag_id PRIMARY KEY (vulnerability_custom_cvss_vector_tag_id),
    CONSTRAINT vulnerability_custom_cvss_vector_tag_cvss_fk FOREIGN KEY (vulnerability_custom_cvss_vector_id) REFERENCES vulnerability_custom_cvss_vector (vulnerability_custom_cvss_vector_id),
    CONSTRAINT vulnerability_custom_cvss_vector_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
    CONSTRAINT vulnerability_custom_cvss_vector_tag_uk UNIQUE (vulnerability_custom_cvss_vector_id, tag_id)
);

CREATE TABLE vulnerability_custom_cvss_severity
(
    vulnerability_custom_cvss_severity_id VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50) NOT NULL,
    refid VARCHAR(20) NOT NULL,
    component_id_format VARCHAR(10),
    component_id_coordinates_json VARCHAR(1000),
    severity FLOAT NOT NULL,
    last_updated_by_username VARCHAR(256) NOT NULL,
    last_updated_at TIMESTAMP NOT NULL,
    CONSTRAINT vulnerability_custom_cvss_severity_pk PRIMARY KEY (vulnerability_custom_cvss_severity_id),
    CONSTRAINT vulnerability_custom_cvss_severity_uk UNIQUE (owner_id, refid, component_id_format, component_id_coordinates_json)
);

CREATE TABLE vulnerability_custom_cvss_severity_tag
(
    vulnerability_custom_cvss_severity_tag_id VARCHAR(50) NOT NULL,
    vulnerability_custom_cvss_severity_id VARCHAR(50) NOT NULL,
    tag_id VARCHAR(50) NOT NULL,
    CONSTRAINT vulnerability_custom_cvss_severity_tag_id PRIMARY KEY (vulnerability_custom_cvss_severity_tag_id),
    CONSTRAINT vulnerability_custom_cvss_severity_tag_cvss_fk FOREIGN KEY (vulnerability_custom_cvss_severity_id) REFERENCES vulnerability_custom_cvss_severity (vulnerability_custom_cvss_severity_id),
    CONSTRAINT vulnerability_custom_cvss_severity_tag_tag_fk FOREIGN KEY (tag_id) REFERENCES tag (tag_id),
    CONSTRAINT vulnerability_custom_cvss_severity_tag_uk UNIQUE (vulnerability_custom_cvss_severity_id, tag_id)
);
