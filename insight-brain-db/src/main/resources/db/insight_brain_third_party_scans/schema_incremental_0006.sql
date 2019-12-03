-- Since 1.79.0
CREATE TABLE coordinate_license (
  coordinate_license_id VARCHAR(50) NOT NULL,
  file_coordinate_id VARCHAR(50) NOT NULL,
  expression VARCHAR(1000) NULL,
  license_id VARCHAR(50) NOT NULL,
  name VARCHAR(50) NULL,
  url VARCHAR(200) NULL,
  CONSTRAINT license_coordinate_pk PRIMARY KEY (coordinate_license_id),
  CONSTRAINT license_coordinate_uk UNIQUE (license_id, file_coordinate_id),
  CONSTRAINT license_coordinate_fk FOREIGN KEY (file_coordinate_id) REFERENCES file_coordinate(file_coordinate_id)
);
