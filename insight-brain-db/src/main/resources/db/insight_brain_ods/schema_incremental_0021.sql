SET SCHEMA insight_brain_ods;

CREATE TABLE policy_waiver (
  policy_waiver_id varchar(50) NOT NULL,
  hash varchar(20) NOT NULL,
  policy_id varchar(50) NOT NULL,
  constraint_id varchar(50) NULL,
  owner_id varchar(50) NOT NULL,
  comment varchar(1000) NULL,
  create_time datetime NOT NULL,
  CONSTRAINT policy_waiver_pk PRIMARY KEY (policy_waiver_id),
  CONSTRAINT policy_waiver_uk UNIQUE KEY (hash, policy_id, constraint_id, owner_id)
);
