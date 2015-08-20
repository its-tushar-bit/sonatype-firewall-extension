-- Since 1.17.0
SET SCHEMA insight_brain_ods;

ALTER TABLE policy_waiver DROP CONSTRAINT policy_waiver_uk;

ALTER TABLE policy_waiver DROP COLUMN constraint_id;

ALTER TABLE policy_waiver ADD CONSTRAINT policy_waiver_uk UNIQUE KEY (hash, policy_id, owner_id);
