-- SaaS Compatible
CREATE TABLE vulnerability_exploitability (
  vulnerability_exploitability_id VARCHAR(50) NOT NULL,
  coordinate_security_id VARCHAR(50) NOT NULL,
  ref_id VARCHAR(20) NOT NULL,
  state VARCHAR(50),
  justification VARCHAR(200),
  response VARCHAR(50),
  detail TEXT,
  CONSTRAINT vulnerability_exploitability_pk PRIMARY KEY (vulnerability_exploitability_id),
  CONSTRAINT vulnerability_exploitability_coordinate_security_fk FOREIGN KEY (coordinate_security_id)
      REFERENCES coordinate_security (coordinate_security_id)
);