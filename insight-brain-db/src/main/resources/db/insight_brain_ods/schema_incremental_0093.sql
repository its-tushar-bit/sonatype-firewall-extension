-- Since 1.18.0
SET SCHEMA insight_brain_ods;

DROP INDEX repository_policy_violation_hash_idx;
CREATE INDEX repository_policy_violation_pathname_idx ON repository_policy_violation(pathname);
