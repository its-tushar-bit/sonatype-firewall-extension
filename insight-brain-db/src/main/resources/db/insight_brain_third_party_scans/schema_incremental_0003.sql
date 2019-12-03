-- Since 1.72.0
DROP TABLE third_party_scan;

CREATE TABLE third_party_scan (
  third_party_scan_id VARCHAR(50) NOT NULL,
  third_party_file_id VARCHAR(50) NOT NULL,
  scan_request_id VARCHAR(50) NOT NULL,
  scan_id VARCHAR(50) NULL,
  create_time TIMESTAMP NOT NULL,
  CONSTRAINT third_party_scan_pk PRIMARY KEY (third_party_scan_id),
  CONSTRAINT third_party_scan_uk UNIQUE (third_party_file_id, scan_request_id),
  CONSTRAINT third_party_scan_fk FOREIGN KEY (third_party_file_id) REFERENCES third_party_file (third_party_file_id)
);
CREATE INDEX third_party_scan_scan_id ON third_party_scan (scan_id);
CREATE INDEX third_party_scan_scan_request_id ON third_party_scan (scan_request_id);
