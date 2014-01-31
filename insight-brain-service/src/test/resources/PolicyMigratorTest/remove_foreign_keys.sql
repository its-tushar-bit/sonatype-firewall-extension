SET SCHEMA insight_brain_ods;

ALTER TABLE policy_waiver
  DROP CONSTRAINT policy_waiver_policy_fk;

ALTER TABLE policy_tag
  DROP CONSTRAINT policy_tag_policy_fk;
