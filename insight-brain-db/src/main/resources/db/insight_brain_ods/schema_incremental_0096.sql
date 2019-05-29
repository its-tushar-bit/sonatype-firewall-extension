-- Since 1.20.0
CREATE TABLE sv_override (
  sv_override_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  source varchar(10) NOT NULL,
  reference_id varchar(20) NOT NULL,
  status varchar(20) NOT NULL,
  comment varchar(1000) NULL,
  CONSTRAINT sv_override_pk PRIMARY KEY (sv_override_id),
  CONSTRAINT sv_override_uk UNIQUE KEY (owner_id, hash, source, reference_id)
);
