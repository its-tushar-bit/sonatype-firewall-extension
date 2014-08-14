-- Since 1.12
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_violation
  ADD COLUMN waived bool DEFAULT false NOT NULL;
  
CREATE TABLE waived_policy_violation (
  policy_violation_id varchar(50) NOT NULL,
  policy_waiver_id varchar(50) NOT NULL,
  comment varchar(1000) NULL,
  CONSTRAINT waived_policy_violation_pk PRIMARY KEY (policy_violation_id),
  CONSTRAINT waived_policy_violation_violation_fk FOREIGN KEY (policy_violation_id) REFERENCES policy_violation(policy_violation_id)
);
  