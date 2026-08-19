CREATE TABLE third_party_vulnerability (
  third_party_vulnerability_id VARCHAR(50) NOT NULL,
  ref_id VARCHAR(20) NOT NULL,
  description TEXT,
  link VARCHAR(200),
  severity FLOAT NOT NULL,
  fixed_by VARCHAR(200),
  vulnerability_source VARCHAR(10),
  severity_description VARCHAR(15),
  attack_vector VARCHAR(100),
  rating_method VARCHAR(10),
  cwes TEXT,
  recommendations TEXT,
  advisories TEXT,
  update_time timestamp, -- when was this vulnerability information last updated
  CONSTRAINT third_party_vulnerability_pk PRIMARY KEY (third_party_vulnerability_id),
  CONSTRAINT third_party_vulnerability_refid_uk UNIQUE (ref_id)
);
