-- since 1.174
-- SaaS compatible
CREATE TABLE sbom_metadata (
    sbom_metadata_id VARCHAR(50) NOT NULL,
    third_party_file_id VARCHAR(50) NOT NULL,
    application_id VARCHAR(50) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    serial_number VARCHAR(2000) NULL,
    sbom_version VARCHAR(50) NOT NULL,
    application_version VARCHAR(200) NULL,
    spec VARCHAR(50) NOT NULL,
    spec_format VARCHAR(50) NOT NULL,
    spec_version VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT sbom_metadata_pk PRIMARY KEY (sbom_metadata_id),
    CONSTRAINT sbom_metadata_third_party_file_fk FOREIGN KEY (third_party_file_id)
        REFERENCES third_party_file (third_party_file_id),
    CONSTRAINT sbom_metadata_uk UNIQUE (application_id, sbom_version)
);

CREATE INDEX application_id_idx ON sbom_metadata(application_id);

ALTER TABLE coordinate_security ADD identification_sources VARCHAR(100) NULL;

ALTER TABLE coordinate_license ADD identification_sources VARCHAR(100) NULL;

ALTER TABLE vulnerability_exploitability ADD created_at TIMESTAMP;

ALTER TABLE vulnerability_exploitability ADD updated_at TIMESTAMP;
