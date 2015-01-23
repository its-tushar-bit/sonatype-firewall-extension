-- Since 1.13.0
SET SCHEMA insight_brain_ods;

CREATE TABLE license_override_license (
  license_override_license_id varchar(50) NOT NULL,
  license_override_id varchar(50) NOT NULL,
  license_id varchar(1000) NOT NULL,
  CONSTRAINT license_override_license_pk PRIMARY KEY (license_override_license_id),
  CONSTRAINT license_override_license_uk UNIQUE KEY (license_override_id, license_id),
  CONSTRAINT license_override_license_override_fk FOREIGN KEY (license_override_id) REFERENCES license_override(license_override_id)
);

INSERT INTO license_override_license(license_override_license_id, license_override_id, license_id) SELECT license_override_id, license_override_id, license_id FROM license_override WHERE license_id is not null;

ALTER TABLE license_override DROP COLUMN license_id;
